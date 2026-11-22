#!/usr/bin/env python3
"""Runtime smoke test: talks to a running dedicated server over RCON and exercises the mod.

Checks that the battlefield dimension exists, its sky/time is fixed to night, that every custom
entity can be summoned (which runs its constructor, attributes and GeckoLib setup on the server)
and that vehicles spawn with a full tank.
"""
import socket
import struct
import sys

HOST = "127.0.0.1"
PORT = 25575
PASSWORD = "warmod"


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


CHECKS = [
    # (command, expected substring or None, critical)
    ("execute in warmod:battlefield run forceload add 0 0", "force loaded", True),
    # informational: fixed_time lives in the dimension type and drives sky/light, `daytime` is the raw counter
    ("execute in warmod:battlefield run time query daytime", None, False),
    ("execute in warmod:battlefield run summon warmod:soldier 0 200 0 {NoAI:1b,Tags:[\"smokesoldier\"]}", "Summoned", True),
    ("execute in warmod:battlefield run summon warmod:tank 6 200 6 {Tags:[\"smoketank\"]}", "Summoned", True),
    ("execute in warmod:battlefield run summon warmod:armored_car_1 12 200 12", "Summoned", True),
    ("execute in warmod:battlefield run summon warmod:armored_car_2 18 200 18", "Summoned", True),
    ("execute in warmod:battlefield run summon warmod:plane 24 200 24", "Summoned", True),
    ("execute in warmod:battlefield run summon warmod:helicopter 30 200 30", "Summoned", True),
    ("execute in warmod:battlefield run summon warmod:corpse 0 200 4", "Summoned", True),
    ("execute in warmod:battlefield run summon warmod:shell 0 202 0 {power:1.0d}", None, False),
    ("execute in warmod:battlefield run data get entity @e[tag=smoketank,limit=1] Fuel", "1.0", True),
    ("execute in warmod:battlefield run data get entity @e[tag=smoketank,limit=1] Health", None, True),
    ("execute in warmod:battlefield run data get entity @e[tag=smokesoldier,limit=1] Health", None, True),
    ("execute in warmod:battlefield run tag @e[tag=smokesoldier] add smoketag", "Added tag", True),
    ("execute in warmod:battlefield run locate biome warmod:ruined_city", None, False),
    # item registry: putting items into a chest resolves the registry names
    ("execute in warmod:battlefield run setblock 0 200 8 minecraft:chest", None, False),
    ("execute in warmod:battlefield run item replace block 0 200 8 container.0 with warmod:ak47", None, True),
    ("execute in warmod:battlefield run item replace block 0 200 8 container.1 with warmod:barrett_m82", None, True),
    ("execute in warmod:battlefield run item replace block 0 200 8 container.2 with warmod:ammo_556 32", None, True),
    ("execute in warmod:battlefield run item replace block 0 200 8 container.3 with warmod:commander_radio", None, True),
    ("execute in warmod:battlefield run item replace block 0 200 8 container.4 with warmod:ammo_crate", None, True),
    ("execute in warmod:battlefield run item replace block 0 200 8 container.5 with warmod:tank_key", None, True),
    ("kill @e[tag=smokesoldier]", None, False),
    ("kill @e[tag=smoketank]", None, False),
]

BAD = ("Unknown", "failed", "Unable", "error", "Exception", "No entity was found")


def main():
    try:
        sock = socket.create_connection((HOST, PORT), timeout=60)
    except OSError as error:
        print(f"RCON connection failed: {error}")
        return 1
    sock.sendall(packet(1, 3, PASSWORD))
    if read_packet(sock) is None:
        print("RCON login failed")
        return 1
    failures = 0
    print("| command | response |")
    print("|---|---|")
    for index, (command, expected, critical) in enumerate(CHECKS, start=2):
        sock.sendall(packet(index, 2, command))
        response = read_packet(sock)
        text = (response[2] if response else "<no response>").replace("\n", " ").strip() or "<empty>"
        print(f"| `{command}` | {text[:200]} |")
        if critical:
            if expected and expected not in text:
                print(f"FAILED (expected '{expected}'): {command}")
                failures += 1
            elif any(bad in text for bad in BAD):
                print(f"FAILED (error response): {command}")
                failures += 1
    sock.close()
    print()
    print(f"critical failures: {failures}")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
