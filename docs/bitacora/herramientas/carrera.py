"""Prueba de doble gasto: compras simultaneas de dos macetas distintas con monedas para una sola."""
import json
import threading
import urllib.request

import psycopg

DB = "host=127.0.0.1 port=5432 dbname=plantia user=plantia password=plantia"
BASE = "http://127.0.0.1:8080/api/v1"
uid, tok = open("lucia.txt").read().split()
H = {"X-User-Id": uid, "X-Session-Token": tok}


def pedir(metodo, ruta):
    req = urllib.request.Request(BASE + ruta, method=metodo, headers=H)
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, json.loads(r.read() or b"null")
    except urllib.error.HTTPError as e:
        return e.code, None


for intento in range(1, 6):
    with psycopg.connect(DB) as c:
        c.execute("update usuarios set monedas = 150 where id_usuario = %s", (int(uid),))
        c.execute("delete from inventario_usuario where id_usuario = %s and id_item in (10, 11)", (int(uid),))
    barrera = threading.Barrier(16)
    codigos = []

    def comprar(item):
        barrera.wait()
        codigos.append((item, pedir("POST", f"/tienda/{item}/comprar")[0]))

    hilos = [threading.Thread(target=comprar, args=(10 if i % 2 else 11,)) for i in range(16)]
    for h in hilos:
        h.start()
    for h in hilos:
        h.join()
    with psycopg.connect(DB) as c:
        monedas = c.execute("select monedas from usuarios where id_usuario = %s", (int(uid),)).fetchone()[0]
        items = [r[0] for r in c.execute("select id_item from inventario_usuario where id_usuario = %s and id_item in (10, 11)", (int(uid),))]
    ok = sorted({i for i, s in codigos if s == 200})
    resumen = {s: sum(1 for _, x in codigos if x == s) for s in sorted({x for _, x in codigos})}
    pagado = 150 - monedas
    valor = 80 * (10 in items) + 100 * (11 in items)
    print(f"intento {intento}: codigos={resumen} items={sorted(items)} monedas_final={monedas} pagado={pagado} valor_recibido={valor}"
          + ("  <-- DOBLE GASTO" if valor > pagado else ""))
