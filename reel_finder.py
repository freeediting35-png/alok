import os
import json
import time
import shutil
import cv2
import numpy as np
from duckduckgo_search import DDGS
import yt_dlp

HISTORY_FILE = "history.txt"
TEMP_DIR = "temp_downloads"
DOWNLOAD_DIR = "downloads"
CATEGORIES = ["horror", "comedy", "short film", "romantic", "action"]

def load_history():
    if os.path.exists(HISTORY_FILE):
        with open(HISTORY_FILE, "r") as f:
            return set(line.strip() for line in f if line.strip())
    return set()

def save_history(url):
    with open(HISTORY_FILE, "a") as f:
        f.write(url + "\n")

def search_reels(category, num_results=20):
    query = f"{category} movie reel hindi -site:youtube.com site:instagram.com OR site:facebook.com"
    results = []

    try:
        ddgs = DDGS()
        for r in ddgs.text(query, max_results=num_results):
            if r and 'href' in r:
                results.append(r['href'])
    except Exception as e:
        print(f"Error searching for {category}: {e}")

    return results

def get_video_metadata(url):
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
        'extract_flat': False,
        'skip_download': True,
    }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            if not info:
                return None

            duration = info.get('duration', 0)
            views = info.get('view_count', 'N/A')
            upload_date = info.get('upload_date', 'N/A')
            uploader = info.get('uploader', 'N/A')
            hashtags = info.get('tags', [])

            return {
                'url': url,
                'duration': duration,
                'views': views,
                'upload_date': upload_date,
                'uploader': uploader,
                'hashtags': hashtags,
                'title': info.get('title', 'N/A'),
                'ext': info.get('ext', 'mp4')
            }
    except Exception as e:
        return None

def download_video(url, output_path):
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
        'outtmpl': output_path,
        'format': 'best',
    }
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.download([url])
        return True
    except Exception as e:
        print(f"Failed to download {url}: {e}")
        return False

def has_watermark(video_path):
    """
    Checks if a video has a static watermark.
    """
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        return False

    frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    fps = int(cap.get(cv2.CAP_PROP_FPS))

    if frame_count <= 0:
        return False

    num_samples = min(20, frame_count)
    step = max(1, frame_count // num_samples)

    samples = []

    for i in range(num_samples):
        cap.set(cv2.CAP_PROP_POS_FRAMES, i * step)
        ret, frame = cap.read()
        if ret:
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            samples.append(gray)

    cap.release()

    if len(samples) < 2:
        return False

    samples_array = np.array(samples)
    variance = np.var(samples_array, axis=0)

    h, w = variance.shape
    cw, ch = int(w * 0.15), int(h * 0.15)

    corners = [
        variance[0:ch, 0:cw],         # Top-Left
        variance[0:ch, w-cw:w],       # Top-Right
        variance[h-ch:h, 0:cw],       # Bottom-Left
        variance[h-ch:h, w-cw:w]      # Bottom-Right
    ]

    for corner in corners:
        static_pixels = np.sum(corner < 5)
        total_pixels = corner.size

        if total_pixels > 0 and (static_pixels / total_pixels) > 0.4:
            return True

    return False

def main():
    history = load_history()
    print(f"Loaded {len(history)} items from history.")

    if not os.path.exists(TEMP_DIR):
        os.makedirs(TEMP_DIR)

    for category in CATEGORIES:
        print(f"\n--- Searching for Category: {category.upper()} ---")
        category_dir = os.path.join(DOWNLOAD_DIR, category)
        if not os.path.exists(category_dir):
            os.makedirs(category_dir)

        urls = search_reels(category)
        print(f"Found {len(urls)} potential links for {category}.")

        found_clean_reel = False
        for url in urls:
            if found_clean_reel:
                break

            if url in history:
                print(f"Skipping already processed URL: {url}")
                continue

            print(f"\nChecking URL: {url}")
            meta = get_video_metadata(url)

            if not meta:
                print("Could not extract metadata.")
                continue

            if meta['duration'] is None or meta['duration'] > 300:
                print(f"Duration too long ({meta['duration']}s), skipping.")
                save_history(url)
                history.add(url)
                continue

            print("Metadata Details:")
            print(f"  Title: {meta['title']}")
            print(f"  Uploader: {meta['uploader']}")
            print(f"  Upload Date: {meta['upload_date']}")
            print(f"  Views: {meta['views']}")
            print(f"  Duration: {meta['duration']}s")
            print(f"  Hashtags: {meta['hashtags']}")
            print(f"  URL: {url}")

            temp_file = os.path.join(TEMP_DIR, f"temp_{int(time.time())}.{meta['ext']}")
            print("Downloading for watermark check...")

            if download_video(url, temp_file):
                print("Checking for watermark...")
                if has_watermark(temp_file):
                    print("Watermark detected! Discarding video.")
                    os.remove(temp_file)
                else:
                    print("Video is clean! Saving to category folder.")
                    final_filename = f"{meta['uploader'].replace('/', '_')}_{int(time.time())}.{meta['ext']}"
                    final_path = os.path.join(category_dir, final_filename)
                    shutil.move(temp_file, final_path)
                    found_clean_reel = True
            else:
                print("Download failed.")

            save_history(url)
            history.add(url)

        if not found_clean_reel:
            print(f"Could not find a clean reel for category {category} today.")

if __name__ == "__main__":
    main()
