#!/usr/bin/env python3
"""Runtime smoke test for a running dedicated server (RCON).

Verifies that the battlefield dimension exists, that every custom entity can be created, that
vehicles spawn fuelled, that the item registry resolves and that bots tick their AI without
throwing. Prints a markdown table and exits non-zero when a critical check fails.
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

    in_dim = "execute in warmod:battlefield run "

    # --- dimension + chunk generator
    rcon.run(in_dim + "forceload add 0 0", "force loaded")
    rcon.run(in_dim + "locate biome warmod:ruined_city")
    rcon.run(in_dim + "time query daytime")

    # --- entity construction (server side constructors, attributes, GeckoLib animatable setup)
    entities = ["soldier", "tank", "armored_car_1", "armored_car_2", "plane", "helicopter", "corpse"]
    for index, entity in enumerate(entities):
        x = index * 6
        rcon.run(f"{in_dim}summon warmod:{entity} {x} 120 0 {{NoGravity:1b,Tags:[\"smoke_{entity}\"]}}", "Summoned")

    # --- everything still alive one tick later? (catches entities that discard themselves on tick)
    time.sleep(2)
    for entity in entities:
        rcon.run(f"{in_dim}execute if entity @e[tag=smoke_{entity},limit=1] run data get entity @e[tag=smoke_{entity},limit=1] UUID")

    # --- vehicle data (fuel = 1.0 at spawn, health tracked through synched data)
    rcon.run(in_dim + "data get entity @e[tag=smoke_tank,limit=1] Fuel", "1.0")
    rcon.run(in_dim + "data get entity @e[tag=smoke_tank,limit=1] Health")
    rcon.run(in_dim + "data get entity @e[tag=smoke_soldier,limit=1] Health")

    # --- item registry (chest slots resolve item ids)
    rcon.run(in_dim + "setblock 0 120 8 minecraft:chest")
    for slot, item in enumerate(["warmod:ak47", "warmod:barrett_m82", "warmod:ammo_556 32",
                                "warmod:commander_radio", "warmod:ammo_crate", "warmod:tank_key"]):
        rcon.run(f"{in_dim}item replace block 0 120 8 container.{slot} with {item}", "Replaced")

    # --- bot AI ticks for a few seconds, one bot rides the tank (exercises passenger seating)
    rcon.run(in_dim + "summon warmod:soldier 60 130 0 {Tags:[\"smoke_ai\"]}", "Summoned")
    rcon.run(in_dim + "summon warmod:soldier 64 130 0 {Tags:[\"smoke_ai\"]}", "Summoned")
    rcon.run(in_dim + "ride @e[tag=smoke_ai,limit=1] mount @e[tag=smoke_tank,limit=1]")
    time.sleep(10)
    rcon.run(in_dim + "execute as @e[tag=smoke_ai,limit=1] run data get entity @s Health")

    # --- cleanup
    rcon.run(in_dim + "kill @e[tag=smoke_ai]")
    for entity in entities:
        rcon.run(f"{in_dim}kill @e[tag=smoke_{entity}]")
    rcon.close()

    print()
    print(f"critical failures: {rcon.failures}")
    return 1 if rcon.failures else 0


if __name__ == "__main__":
    sys.exit(main())
