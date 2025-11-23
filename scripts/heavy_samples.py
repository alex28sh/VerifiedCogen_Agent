import json
import urllib.request

# ---- configure your source here ----
# Option A: from URL
# url = "https://example.com/data.json"
# with urllib.request.urlopen(url) as response:
#     data = json.load(response)

# Option B: from local file
with open("results/results_0_Mode2/run1/0_Mode2_1_tokens.json", "r") as f:
    data = json.load(f)

# ---- processing ----
for name, values in data.items():
    first = values.get("first", 0)
    second = values.get("second", 0)

    if first + second > 20_000:
        print(name)

