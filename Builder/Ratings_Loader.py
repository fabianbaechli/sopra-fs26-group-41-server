class RatingLoader:
    def __init__(self, file_path):
        self.Ratings_Location = file_path

    def stream_user_ratings(self):
        current_user_id = None
        user_ratings = {}

        with open(self.Ratings_Location, 'r', encoding='utf-8') as file:
            next(file)
            
            for line in file:
                parts = line.strip().split(',')
                if len(parts) < 3:
                    continue
                    
                user_id = int(parts[0])
                movie_id = int(parts[1])
                
                # Pre-calculate the integer rating
                rating = int(round(float(parts[2]) * 10))

                
                if current_user_id is None:
                    current_user_id = user_id

                if user_id != current_user_id:
                    yield current_user_id, user_ratings
                    
                    
                    current_user_id = user_id
                    user_ratings = {}

                user_ratings[movie_id] = rating
            

            if current_user_id is not None and user_ratings:
                yield current_user_id, user_ratings