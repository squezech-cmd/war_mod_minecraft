#!/usr/bin/env python3
"""Runtime smoke test for a running dedicated server (RCON).

Verifies the battlefield dimension, entity construction, vehicle fuel, item registry and bot AI.
A vanilla armor stand is used as a control to separate level/selector problems from mod problems.
Prints a markdown table and exits non-zero when a critical check fails.
"""
import socket
import struct
import sys
import time

HOST = "127.0.0.1"
PORT = 25575
PASSWORD = "warmod"
BAD = ("Unknown", "failed", "Unable", "error", "Exception", "No entity was found", "That position")


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
        print(f"| `{command}` | {text[:220]} |")
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
    dim = "execute in warmod:battlefield run "

    # ---------------- level / chunk generator / biome
    rcon.run(dim + "forceload add 0 0", "force loaded")
    rcon.run(dim + "locate biome warmod:ruined_city")
    rcon.run(dim + "time query daytime")

    # ---------------- control group: vanilla entity in the same dimension
    rcon.run(dim + 'summon minecraft:armor_stand 2 120 0 {NoGravity:1b,Tags:["ctl"]}', "Summoned")
    rcon.run(dim + "data get entity @e[tag=ctl,limit=1] Pos")
    # ---------------- mod entities, queried immediately (same tick batch)
    rcon.run(dim + 'summon warmod:tank 6 120 0 {NoGravity:1b,Tags:["t1"]}', "Summoned")
    rcon.run(dim + "data get entity @e[tag=t1,limit=1] Pos")
    rcon.run(dim + 'summon warmod:soldier 10 120 0 {NoAI:1b,NoGravity:1b,Tags:["s1"]}', "Summoned")
    rcon.run(dim + "data get entity @e[tag=s1,limit=1] Pos")

    time.sleep(3)
    rcon.run(dim + "data get entity @e[tag=ctl,limit=1] Pos")
    rcon.run(dim + "data get entity @e[tag=t1,limit=1] Pos")
    rcon.run(dim + "data get entity @e[tag=s1,limit=1] Pos")
    rcon.run(dim + 'execute if entity @e[tag=ctl] run say CONTROL_ALIVE')
    rcon.run(dim + 'execute if entity @e[tag=t1] run say TANK_ALIVE')
    time.sleep(3)
    rcon.run(dim + 'execute if entity @e[tag=ctl] run say CONTROL_ALIVE_LATE')
    rcon.run(dim + 'execute if entity @e[tag=t1] run say TANK_ALIVE_LATE')

    # ---------------- fuel / health when the vehicle is still there
    rcon.run(dim + "data get entity @e[tag=t1,limit=1] Fuel", "1.0")
    rcon.run(dim + "data get entity @e[tag=t1,limit=1] Health")
    rcon.run(dim + "data get entity @e[tag=s1,limit=1] Health")

    # ---------------- every other entity type
    for index, entity in enumerate(["armored_car_1", "armored_car_2", "plane", "helicopter", "corpse"]):
        rcon.run(dim + f'summon warmod:{entity} {20 + index * 6} 120 0 {{NoGravity:1b,Tags:["e_{entity}"]}}', "Summoned")
    time.sleep(2)
    for entity in ["armored_car_1", "armored_car_2", "plane", "helicopter", "corpse"]:
        rcon.run(dim + f"data get entity @e[tag=e_{entity},limit=1] Pos")
    rcon.run(dim + "data get entity @e[tag=e_plane,limit=1] Fuel", "1.0")

    # ---------------- item registry
    rcon.run(dim + "setblock 0 120 8 minecraft:chest")
    for slot, item in enumerate(["warmod:ak47", "warmod:barrett_m82", "warmod:ammo_556 32",
                                "warmod:commander_radio", "warmod:ammo_crate", "warmod:tank_key"]):
        rcon.run(f"{dim}item replace block 0 120 8 container.{slot} with {item}", "Replaced")

    # ---------------- bot AI for a few seconds
    rcon.run(dim + 'summon warmod:soldier 60 130 0 {Tags:["ai1"]}', "Summoned")
    rcon.run(dim + 'summon warmod:soldier 64 130 0 {Tags:["ai2"]}', "Summoned")
    time.sleep(10)
    rcon.run(dim + "data get entity @e[tag=ai1,limit=1] Health")
    rcon.run(dim + "ride @e[tag=ai1,limit=1] mount @e[tag=t1,limit=1]")

    # ---------------- cleanup
    rcon.run(dim + "kill @e[tag=ai1]")
    rcon.run(dim + "kill @e[tag=ai2]")
    rcon.run(dim + "kill @e[tag=ctl]")
    rcon.close()

    print()
    print(f"critical failures: {rcon.failures}")
    return 1 if rcon.failures else 0


if __name__ == "__main__":
    sys.exit(main())
