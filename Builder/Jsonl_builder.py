from Ratings_Loader import RatingLoader
import pickle
import json
import os
import itertools
from collections import defaultdict

class Manager:
    def __init__(self, max_gb: float):
        self.Ratings_Csv_Location = "ratings.csv"
            
        bytes_available = max_gb * 1073741824 * 0.8
        self.max_dict_items = int(bytes_available // 188)
        
        print(f"Memory Limit: {max_gb} GB. Max in-memory edges: {self.max_dict_items:,}")

        self.edges = {} # Stores: edge_id: update_val (rating_sum and count packed)
        self.chunk_count = 0
        self.temp_folder = "temp_chunks"
        self.num_shards = 16 # Number of shards to split chunks into
        
        if not os.path.exists(self.temp_folder):
            os.makedirs(self.temp_folder)

    def build_graph(self):
        ratings_loader = RatingLoader(self.Ratings_Csv_Location)
        user_count = 0
        
        # Stream one user at a time directly from the CSV
        for user_id, user_ratings in ratings_loader.stream_user_ratings():
            user_count += 1
            if user_count % 1000 == 0:                
                print(f"\rBuilding graph... Percentage {user_count/330975:.2f} Processed {user_count:,} users", end="", flush=True)

            
            for m1, m2 in itertools.combinations(user_ratings.keys(), 2):
                
                combined_rating = user_ratings[m1] + user_ratings[m2]
                
                edge_id = (m1 << 32) | m2 if m1 < m2 else (m2 << 32) | m1
                
                update_val = (combined_rating << 32) | 1
                
                if edge_id in self.edges:
                    self.edges[edge_id] += update_val
                else:
                    self.edges[edge_id] = update_val
            
            if len(self.edges) >= self.max_dict_items:
                self.flush_chunk()
                
        if self.edges:
            self.flush_chunk()
            
        print(f"\nGraph building complete. Processed {user_count:,} total users. Moving to merge phase.")

    def flush_chunk(self):
        self.chunk_count += 1
        shards = [{} for _ in range(self.num_shards)]
        
        for edge_id, update_val in self.edges.items():
            shard_idx = edge_id % self.num_shards
            shards[shard_idx][edge_id] = update_val
            
        for i, shard_data in enumerate(shards):
            chunk_path = os.path.join(self.temp_folder, f"chunk_{self.chunk_count}_shard_{i}.pkl")
            with open(chunk_path, 'wb') as f:
                pickle.dump(shard_data, f, protocol=pickle.HIGHEST_PROTOCOL)
                
        print(f"\n[Memory Check] Flushed {len(self.edges):,} edges across {self.num_shards} shards.")
        self.edges.clear()

    def merge_chunks_and_save(self, final_file_name="final_movie_graph.jsonl"):
        print("Merging chunks by shard")
        
        with open(final_file_name, 'w', encoding='utf-8') as out_file:
            
            for s in range(self.num_shards):
                shard_graph = defaultdict(int) 
                
                for c in range(1, self.chunk_count + 1):
                    chunk_path = os.path.join(self.temp_folder, f"chunk_{c}_shard_{s}.pkl")
                    if not os.path.exists(chunk_path):
                        continue
                        
                    with open(chunk_path, 'rb') as f:
                        chunk_data = pickle.load(f)
                        
                    for edge_id, update_val in chunk_data.items():
                        shard_graph[edge_id] += update_val
                        


                for edge_id, merged_val in shard_graph.items():
                    rating_sum = merged_val >> 32
                    count = merged_val & 0xFFFFFFFF
                    
                    json_line = json.dumps({str(edge_id): [rating_sum, count]})
                    out_file.write(json_line + '\n')
                    
                print(f"Merged and saved shard {s+1}/{self.num_shards}")
                
        print(f"Successfully merged and saved final graph to {final_file_name}")

if __name__=="__main__":
    manager = Manager(max_gb=45) 
    manager.build_graph()
    manager.merge_chunks_and_save()