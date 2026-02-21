import unittest
import os
from unittest.mock import patch, MagicMock
from main import get_wireless_interfaces, is_monitor_mode, parse_scan_csv, Target

class TestWifiTool(unittest.TestCase):
    @patch('subprocess.check_output')
    def test_get_wireless_interfaces(self, mock_check_output):
        mock_check_output.return_value = b'wlan0     IEEE 802.11  ESSID:off/any  \n          Mode:Managed  Frequency:2.412 GHz  Access Point: Not-Associated   \n          Tx-Power=20 dBm   \n          Retry short limit:7   RTS thr:off   Fragment thr:off\n          Power Management:off\n          \nlo        no wireless extensions.\n\neth0      no wireless extensions.\n\n'
        interfaces = get_wireless_interfaces()
        self.assertEqual(interfaces, ['wlan0'])

    @patch('subprocess.check_output')
    def test_is_monitor_mode(self, mock_check_output):
        mock_check_output.return_value = b'wlan0     IEEE 802.11  Mode:Monitor  Frequency:2.412 GHz  Tx-Power=20 dBm   \n          Retry short limit:7   RTS thr:off   Fragment thr:off\n          Power Management:off\n          \n'
        self.assertTrue(is_monitor_mode('wlan0'))

        mock_check_output.return_value = b'wlan0     IEEE 802.11  Mode:Managed  Frequency:2.412 GHz  Access Point: Not-Associated   \n          Tx-Power=20 dBm   \n          Retry short limit:7   RTS thr:off   Fragment thr:off\n          Power Management:off\n          \n'
        self.assertFalse(is_monitor_mode('wlan0'))

    def test_parse_scan_csv(self):
        # Create a dummy CSV content
        csv_content = """BSSID, First time seen, Last time seen, channel, Speed, Privacy, Cipher, Authentication, Power, # beacons, # IV, LAN IP, ID-length, ESSID, Key
AA:BB:CC:DD:EE:FF, 2023-01-01 12:00:00, 2023-01-01 12:01:00, 6, 54, WPA2, CCMP, PSK, -50, 100, 0, 0.0.0.0, 4, TestAP,
Station MAC, First time seen, Last time seen, Power, # packets, BSSID, Probed ESSIDs
"""
        with open('test_scan.csv', 'w') as f:
            f.write(csv_content)

        targets = parse_scan_csv('test_scan.csv')
        self.assertEqual(len(targets), 1)
        self.assertEqual(targets[0].ssid, 'TestAP')
        self.assertEqual(targets[0].bssid, 'AA:BB:CC:DD:EE:FF')

        if os.path.exists('test_scan.csv'):
            os.remove('test_scan.csv')

if __name__ == '__main__':
    unittest.main()
