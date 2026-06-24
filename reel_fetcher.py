import os
import re
import json
import random
import requests
from bs4 import BeautifulSoup
import yt_dlp
import cv2
import numpy as np

# Platforms to search on (excluding YouTube)
PLATFORMS = ["site:instagram.com/reel/", "site:facebook.com/reel/"]

# Categories
CATEGORIES = {
    "comedy": "hindi comedy movie",
    "horror": "hindi horror movie",
    "short_film": "hindi short film"
}

from ddgs import DDGS

def search_duckduckgo(query):
    """Searches using DuckDuckGo Search (ddgs) and returns a list of URLs."""
    print(f"Searching for: {query}")
    try:
        links = []
        with DDGS() as ddgs:
            for r in ddgs.text(query, max_results=10):
                links.append(r['href'])
        return links
    except Exception as e:
        print(f"Error during search: {e}")
        return []

def get_reels_for_categories():
    results = {}
    for cat_name, cat_query in CATEGORIES.items():
        results[cat_name] = []
        for platform in PLATFORMS:
            # Add a random element to the query to get different results daily
            random_salt = random.choice(["2024", "viral", "trending", "scene"])
            query = f"{platform} \"{cat_query}\" {random_salt}"
            links = search_duckduckgo(query)
            # Filter links to ensure they match the platform roughly
            for link in links:
                if 'instagram.com/reel/' in link or 'facebook.com/reel/' in link:
                     results[cat_name].append(link)
    return results

def has_watermark(video_path):
    """
    A basic watermark detection framework using OpenCV.
    In a real-world scenario, this requires complex AI models or edge detection
    over multiple frames to find static pixels. Here we just open the video
    and provide a placeholder logic.
    """
    try:
        cap = cv2.VideoCapture(video_path)
        if not cap.isOpened():
            return False

        # Read the first frame
        ret, frame = cap.read()
        cap.release()

        if not ret:
            return False

        # Placeholder: Check for typical watermark locations (edges)
        # For a full implementation, you would analyze edge variance over time.
        # We will assume no watermark for this basic check unless a strict condition is met.
        return False
    except Exception as e:
        print(f"Error checking watermark for {video_path}: {e}")
        return False

def extract_metadata_and_download(url, category, history):
    """Extracts metadata and downloads the video using yt-dlp."""
    if url in history:
        print(f"Skipping: Already downloaded {url}")
        return None

    print(f"Processing: {url}")
    ydl_opts = {
        'format': 'best',
        'outtmpl': f'downloads/{category}/%(id)s.%(ext)s', # Keep filename short to prevent OS errors
        'quiet': True,
        'no_warnings': True,
        'dumpjson': True,
        'trim_file_name': 50, # Ensure title isn't too long if we used it
        # 'cookiesfrombrowser': ('chrome',), # Optional: might be needed for IG/FB
    }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)

            # Check duration (< 5 mins)
            duration = info.get('duration', 0)
            if duration and duration > 300:
                print(f"Skipping: Video is longer than 5 minutes ({duration}s)")
                return None

            metadata = {
                'url': url,
                'title': info.get('title'),
                'uploader': info.get('uploader'),
                'upload_date': info.get('upload_date'),
                'view_count': info.get('view_count'),
                'tags': info.get('tags', []),
                'description': info.get('description', '')[:100], # Snippet
                'category': category
            }

            print(f"Found Metadata: Uploader: {metadata['uploader']}, Views: {metadata['view_count']}, Date: {metadata['upload_date']}")

            # Actually download
            ydl_opts['dumpjson'] = False
            with yt_dlp.YoutubeDL(ydl_opts) as ydl_dl:
                info_dict = ydl_dl.extract_info(url, download=True)
                downloaded_file = ydl_dl.prepare_filename(info_dict)

            # Check watermark
            if has_watermark(downloaded_file):
                print(f"Watermark detected in {downloaded_file}. Deleting...")
                os.remove(downloaded_file)
                return None

            # Save metadata alongside the video
            meta_filename = os.path.splitext(downloaded_file)[0] + '.json'
            with open(meta_filename, 'w', encoding='utf-8') as f:
                json.dump(metadata, f, ensure_ascii=False, indent=4)

            print(f"Saved metadata to {meta_filename}")

            return metadata

    except Exception as e:
        print(f"Error extracting/downloading {url}: {e}")
        return None

def load_history(filepath="history.json"):
    if os.path.exists(filepath):
        with open(filepath, "r") as f:
            return json.load(f)
    return []

def save_history(history, filepath="history.json"):
    with open(filepath, "w") as f:
        json.dump(history, f)

if __name__ == "__main__":
    history = load_history()
    reels = get_reels_for_categories()

    # Download 1 per category
    for cat, urls in reels.items():
        print(f"\n--- Processing category: {cat} ---")
        for url in urls:
            meta = extract_metadata_and_download(url, cat, history)
            if meta:
                history.append(url)
                save_history(history)
                print(f"Successfully downloaded 1 reel for {cat}!")
                break # Move to next category
