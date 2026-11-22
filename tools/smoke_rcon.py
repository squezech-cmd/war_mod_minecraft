#!/usr/bin/env python3
"""Runtime smoke test for a running dedicated server (RCON).

Entities only materialise in loaded chunks, so the harness first force-loads the test chunk and waits
until the server reports it as loaded/ticking. Then it checks the battlefield dimension, the chunk
generator, every custom entity, vehicle fuel, the item registry and bot AI.
"""
import socket
import struct
import sys
import time

HOST = "127.0.0.1"
PORT = 25575
PASSWORD = "warmod"
BAD = ("Unknown", "failed", "Unable", "error", "Exception", "No entity was found", "That position")
DIM = "execute in warmod:battlefield run "


def packet(request_id, packet_type, body):
    payload = struct.pack("<ii", request_id, packet_type) + body.encode("utf8") + b"\x00\x00"
    return struct.pack("<i", len(payload)) + payload


def read_packet(sock):
    header = b""
    while len(header) < 4:
        chunk = sock.recv(4 - len(header))
        if not chunk:
            return None
        header += chunk
    (length,) = struct.unpack("<i", header)
    data = b""
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            break
        data += chunk
    request_id, packet_type = struct.unpack("<ii", data[:8])
    return request_id, packet_type, data[8:-2].decode("utf8", "replace")


class Rcon:
    def __init__(self, host=HOST, port=PORT, password=PASSWORD):
        self.sock = socket.create_connection((host, port), timeout=60)
        self.sock.sendall(packet(1, 3, password))
        if read_packet(self.sock) is None:
            raise SystemExit("RCON login failed")
        self.index = 2
        self.failures = 0

    def run(self, command, expected=None, critical=False):
        self.sock.sendall(packet(self.index, 2, command))
        self.index += 1
        response = read_packet(self.sock)
        text = (response[2] if response else "<no response>").replace("\n", " ").strip() or "<empty>"
        print(f"| `{command}` | {text[:240]} |")
        if expected and expected not in text:
            self.failures += 1
            print(f"FAILED (expected '{expected}'): {command}")
        elif critical and any(bad in text for bad in BAD):
            self.failures += 1
            print(f"FAILED (error response): {command}")
        return text

    def close(self):
        self.sock.close()


def main():
    try:
        rcon = Rcon()
    except OSError as error:
        print(f"RCON connection failed: {error}")
        return 1

    print("| command | response |")
    print("|---|---|")

    # ---------------- level, chunk generator, biome
    rcon.run(DIM + "forceload add 0 0", "force loaded")
    rcon.run(DIM + "locate biome warmod:ruined_city")
    rcon.run(DIM + "time query daytime")

    # ---------------- pick a loaded, entity-ticking spot (the dimension spawn area) as the test location
    import re

    spot = None
    for attempt in range(20):
        status = rcon.run("execute in warmod:battlefield run warmod status")
        match = re.search(r"spawn=(-?\d+),(-?\d+),(-?\d+).*spawnTicking=(true|false).*spawnChunkLoaded=(true|false)",
                          status)
        if match:
            x, y, z = (int(match.group(i)) for i in range(1, 4))
            if match.group(4) == "true" and match.group(5) == "true":
                spot = (x, y + 40, z)
                print(f"test spot ready after {attempt + 1} probes: {spot}")
                break
        time.sleep(1)
    if spot is None:
        # fall back to the force-loaded origin so the remaining checks still produce information
        spot = (8, 120, 8)
        rcon.failures += 1
        print("FAILED: no entity-ticking spot found, falling back to (8, 120, 8)")
    sx, sy, sz = spot

    # ---------------- control group: vanilla entity in the battlefield dimension
    rcon.run(DIM + f'summon minecraft:armor_stand {sx} {sy} {sz} {{NoGravity:1b,Tags:["ctl"]}}', "Summoned")
    rcon.run(DIM + "data get entity @e[tag=ctl,limit=1] Pos", "Pos")

    # ---------------- every mod entity type
    for index, entity in enumerate(["soldier", "tank", "armored_car_1", "armored_car_2", "plane", "helicopter", "corpse"]):
        rcon.run(DIM + f'summon warmod:{entity} {sx + 2 + index} {sy} {sz + 2} {{NoGravity:1b,Tags:["e_{entity}"]}}', "Summoned")
    time.sleep(2)
    for entity in ["soldier", "tank", "armored_car_1", "armored_car_2", "plane", "helicopter", "corpse"]:
        rcon.run(DIM + f"data get entity @e[tag=e_{entity},limit=1] Pos", "Pos")

    # ---------------- vehicle fuel / health
    rcon.run(DIM + "data get entity @e[tag=e_tank,limit=1] Fuel", "1.0")
    rcon.run(DIM + "data get entity @e[tag=e_tank,limit=1] Health", "Health")
    rcon.run(DIM + "data get entity @e[tag=e_plane,limit=1] Fuel", "1.0")
    rcon.run(DIM + "data get entity @e[tag=e_soldier,limit=1] Health", "Health")

    # ---------------- item registry
    rcon.run(DIM + f"setblock {sx + 6} {sy} {sz} minecraft:chest", "Changed")
    for slot, item in enumerate(["warmod:ak47", "warmod:barrett_m82", "warmod:ammo_556 32",
                                "warmod:commander_radio", "warmod:ammo_crate", "warmod:tank_key"]):
        rcon.run(f"{DIM}item replace block {sx + 6} {sy} {sz} container.{slot} with {item}", "Replaced")

    # ---------------- bot AI ticks for a few seconds and rides the tank
    rcon.run(DIM + f'summon warmod:soldier {sx + 8} {sy} {sz + 8} {{Tags:["ai1"]}}', "Summoned")
    rcon.run(DIM + f'summon warmod:soldier {sx + 10} {sy} {sz + 8} {{Tags:["ai2"]}}', "Summoned")
    time.sleep(8)
    rcon.run(DIM + "data get entity @e[tag=ai1,limit=1] Health", "Health")
    rcon.run(DIM + "ride @e[tag=ai1,limit=1] mount @e[tag=e_tank,limit=1]")
    time.sleep(2)
    rcon.run(DIM + "data get entity @e[tag=e_tank,limit=1] Passengers")

    # ---------------- cleanup
    for tag in ["ai1", "ai2", "ctl"] + [f"e_{e}" for e in
                                       ["soldier", "tank", "armored_car_1", "armored_car_2", "plane", "helicopter", "corpse"]]:
        rcon.run(DIM + f"kill @e[tag={tag}]")
    rcon.close()

    print()
    print(f"critical failures: {rcon.failures}")
    return 1 if rcon.failures else 0


if __name__ == "__main__":
    sys.exit(main())
