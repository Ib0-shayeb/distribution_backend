import time
import json
import urllib.request
import random

API_URL = "http://localhost:8080/api/locations/log"

# Define base starting coordinates for our two drivers
drivers = {
    1: {"name": "Amjad", "lat": 31.9522, "lon": 35.9150},
    2: {"name": "Driver 1 (Test)", "lat": 31.9630, "lon": 35.9250}
}

print("🚀 Starting Live Fleet Telemetry Simulation...")
print("Press Ctrl+C to terminate transmission loops.")

try:
    step = 0
    while True:
        step += 1
        print(f"\n--- Telemetry Broadcast Generation #{step} ---")
        
        for user_id, coords in drivers.items():
            # Simulate slight continuous driving movement vector changes
            coords["lat"] += random.uniform(-0.0008, 0.0008)
            coords["lon"] += random.uniform(-0.0008, 0.0008)
            
            payload = {
                "userId": user_id,
                "latitude": round(coords["lat"], 6),
                "longitude": round(coords["lon"], 6)
            }
            
            # Send HTTP POST payload request to Spring Boot Ingestion Engine
            req = urllib.request.Request(
                API_URL, 
                data=json.dumps(payload).encode('utf-8'),
                headers={'Content-Type': 'application/json'},
                method='POST'
            )
            
            try:
                with urllib.request.urlopen(req) as response:
                    if response.status == 200:
                        print(f"📡 {coords['name']} (ID {user_id}) -> Lat: {payload['latitude']}, Lon: {payload['longitude']} [OK]")
            except Exception as e:
                print(f"❌ Failed to transmit data payload for driver {user_id}: {e}")
                
        # Send fresh updates down the wire every 4 seconds
        time.sleep(4)

except KeyboardInterrupt:
    print("\n🛑 Telemetry transmitter simulation loops manually suspended.")