"""Run with a short conversation visible on the connected tablet."""
import json
import re
import subprocess


def find_scroll(nodes):
    for node in nodes:
        if node.get("class") == "android.widget.ScrollView":
            return node
        found = find_scroll(node.get("children", []))
        if found:
            return found


layout = json.loads(subprocess.check_output(
    ["android", "layout", "--full"], text=True))
scroll = find_scroll(layout)
assert scroll, "App ScrollView is not visible"
bounds = list(map(int, re.findall(r"-?\d+", scroll["bounds"])))
last = scroll["children"][0]["children"][-1]
bottom = int(re.findall(r"-?\d+", last["bounds"])[-1])
gap = bounds[3] - bottom
assert 0 <= gap <= (bounds[3] - bounds[1]) * 0.05, (
    f"Content is not bottom-aligned: {gap}px gap")
print("PASS: content is bottom-aligned")
