import requests
import re
import threading
import webbrowser
from bs4 import BeautifulSoup
import concurrent.futures
import customtkinter as ctk
from tkinter import ttk

# ---------------------------------------------------------
# Web Scraping Functions
# ---------------------------------------------------------

def fetch_game_data(game):
    """Helper function: Downloads the subpage for a single game and extracts prices."""
    game_url = f"https://www.steamcardexchange.net/index.php?inventorygame-appid-{game['appid']}"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    
    try:
        # Added timeout so threads don't hang on bad connections
        game_resp = requests.get(game_url, headers=headers, timeout=15)
        
        if game_resp.status_code == 200:
            soup = BeautifulSoup(game_resp.text, 'html.parser')
            clean_text = soup.get_text(separator=' ')
            prices_found = re.findall(r'Price \(You Pay\):\s*(\d+)\s*c', clean_text)
            
            if len(prices_found) >= game['cards_in_set']:
                real_prices = [int(p) for p in prices_found[:game['cards_in_set']]]
                real_total = sum(real_prices)
                
                game['real_total'] = real_total
                game['link'] = game_url
                return game
    except Exception:
        # If an error occurs (e.g., timeout), just ignore this game
        pass
        
    return None

# ---------------------------------------------------------
# Modern GUI Application
# ---------------------------------------------------------

class SteamCardApp(ctk.CTk):
    def __init__(self):
        super().__init__()

        self.title("Steam Card Exchange - Deal Finder")
        self.geometry("900x600")
        ctk.set_appearance_mode("dark")  # Modes: "System" (standard), "Dark", "Light"
        ctk.set_default_color_theme("blue")  # Themes: "blue", "green", "dark-blue"

        # --- UI Elements Setup ---
        
        # Header Label
        self.header_label = ctk.CTkLabel(self, text="Steam Card Exchange Price Fetcher", font=ctk.CTkFont(size=20, weight="bold"))
        self.header_label.pack(pady=(20, 10))

        # Fetch Button
        self.fetch_button = ctk.CTkButton(self, text="Fetch Top 50 Deals", command=self.start_fetching)
        self.fetch_button.pack(pady=10)

        # Status Label
        self.status_label = ctk.CTkLabel(self, text="Ready to fetch data.", text_color="gray")
        self.status_label.pack(pady=5)

        # Progress Bar
        self.progress_bar = ctk.CTkProgressBar(self, width=400)
        self.progress_bar.pack(pady=10)
        self.progress_bar.set(0)

        # Table Frame
        self.table_frame = ctk.CTkFrame(self)
        self.table_frame.pack(fill="both", expand=True, padx=20, pady=20)

        # Treeview (Table) setup for displaying results
        style = ttk.Style()
        style.theme_use("default")
        style.configure("Treeview", background="#2b2b2b", foreground="white", rowheight=25, fieldbackground="#2b2b2b")
        style.map('Treeview', background=[('selected', '#1f538d')])
        
        columns = ("Name", "Credits", "Available", "Link")
        self.tree = ttk.Treeview(self.table_frame, columns=columns, show="headings")
        self.tree.heading("Name", text="Game Name")
        self.tree.heading("Credits", text="Credits (You Pay)")
        self.tree.heading("Available", text="Sets Available")
        self.tree.heading("Link", text="URL (Hidden)")

        self.tree.column("Name", width=400)
        self.tree.column("Credits", width=120, anchor="center")
        self.tree.column("Available", width=120, anchor="center")
        self.tree.column("Link", width=0, stretch=False) # Hide the link column visually

        self.tree.pack(fill="both", expand=True)
        
        # Bind double-click event to open the link
        self.tree.bind("<Double-1>", self.open_link)

    def open_link(self, event):
        """Opens the selected game's URL in the default web browser."""
        selected_item = self.tree.selection()
        if selected_item:
            item = self.tree.item(selected_item)
            link = item['values'][3]
            if link:
                webbrowser.open(link)

    def start_fetching(self):
        """Prepares UI for fetching and starts the background thread."""
        self.fetch_button.configure(state="disabled")
        self.tree.delete(*self.tree.get_children()) # Clear existing data
        self.progress_bar.set(0)
        self.status_label.configure(text="Fetching global list from API...")
        
        # Run in a separate thread so the GUI doesn't freeze
        threading.Thread(target=self.fetch_data_process, daemon=True).start()

    def fetch_data_process(self):
        """The main logic for fetching and processing data."""
        api_url = "https://www.steamcardexchange.net/api/request.php?GetInventory"
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        
        response = requests.get(api_url, headers=headers)
        
        if response.status_code != 200:
            self.update_status("Error fetching API data.")
            self.reset_button()
            return
            
        data = response.json()
        rows = data.get('data', []) if isinstance(data, dict) else data
        
        candidates = []
        
        for row in rows:
            try:
                appid = row[0][0]
                name = row[0][1]
                worth = int(row[1])
                set_data = row[3]
                cards_in_set = int(set_data[0])
                
                if set_data[0] == set_data[1]:
                    sets_available = int(set_data[2])
                    if sets_available > 0:
                        candidates.append({
                            'appid': appid,
                            'name': name,
                            'cards_in_set': cards_in_set,
                            'sets_available': sets_available,
                        })
            except (IndexError, ValueError, TypeError):
                continue

        total_games = len(candidates)
        self.update_status(f"Found {total_games} fully available sets. Starting multi-threaded check...")
        
        final_sets = []
        completed = 0
        
        # ThreadPoolExecutor for fast, parallel processing
        # MAX_WORKERS = 20 is a good balance between speed and server protection
        with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
            futures = {executor.submit(fetch_game_data, game): game for game in candidates}
            
            for future in concurrent.futures.as_completed(futures):
                result = future.result()
                if result:
                    final_sets.append(result)
                
                completed += 1
                
                # Update progress bar and status periodically
                if completed % 10 == 0 or completed == total_games:
                    progress = completed / total_games
                    self.progress_bar.set(progress)
                    self.update_status(f"Checked {completed} of {total_games} games...")

        # Sort all found sets by the real calculated price
        final_sets.sort(key=lambda x: x['real_total'])
        
        self.update_status("Done! Displaying top 50 cheapest sets. Double-click a row to open in browser.")
        self.populate_table(final_sets[:50])
        self.reset_button()

    # --- UI Helper Methods ---
    
    def update_status(self, message):
        """Safely updates the status label from a background thread."""
        self.status_label.configure(text=message)

    def reset_button(self):
        """Re-enables the fetch button."""
        self.fetch_button.configure(state="normal")

    def populate_table(self, data):
        """Inserts the sorted data into the Treeview."""
        for item in data:
            self.tree.insert("", "end", values=(
                item['name'], 
                item['real_total'], 
                item['sets_available'], 
                item['link']
            ))

if __name__ == "__main__":
    app = SteamCardApp()
    app.mainloop()