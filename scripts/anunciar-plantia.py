"""Anuncia el backend de Plantia en la red local mediante mDNS.

Con esto la app Android encuentra solo el PC (por ejemplo `plantia.local`)
sin tener que hardcodear la IP: da igual a que WiFi se conecte el equipo,
siempre que el movil y el PC esten en la misma red.

Uso:
    python -m pip install -r scripts/requirements-mdns.txt
    python scripts/anunciar-plantia.py

Publica el servicio `_plantia._tcp.local.` en el puerto 8080.
"""
import argparse
import socket
import sys
import time

try:
    from zeroconf import ServiceInfo, Zeroconf
except ImportError:
    print(
        "Falta la libreria 'zeroconf'. Instala con:\n"
        "    python -m pip install -r scripts/requirements-mdns.txt",
        file=sys.stderr,
    )
    sys.exit(1)

TIPO_SERVICIO = "_plantia._tcp.local."
NOMBRE_SERVICIO = "plantia-backend"
PUERTO = 8080


def ip_local() -> str:
    """IP de la interfaz por la que sale a internet (no 127.0.0.1 ni IPv6)."""
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # No envia datos: solo elige la ruta de salida.
        sock.connect(("8.8.8.8", 80))
        return sock.getsockname()[0]
    except OSError:
        return socket.gethostbyname(socket.gethostname())
    finally:
        sock.close()


def main() -> int:
    parser = argparse.ArgumentParser(description="Anuncia Plantia por mDNS")
    parser.add_argument("--puerto", type=int, default=PUERTO)
    parser.add_argument("--nombre", default=NOMBRE_SERVICIO)
    args = parser.parse_args()

    ip = ip_local()
    print(f"[*] IP local detectada: {ip}")
    print(f"[*] Publicando _plantia._tcp.local. en el puerto {args.puerto}")
    print("[*] Ctrl+C para detener el anuncio\n")

    info = ServiceInfo(
        TIPO_SERVICIO,
        # El nombre de la instancia debe terminar en el tipo de servicio.
        f"{args.nombre}.{TIPO_SERVICIO}",
        addresses=[socket.inet_aton(ip)],
        port=args.puerto,
        properties={"app": "plantia", "api": "/api/v1"},
        server=f"{args.nombre}.local.",
    )

    zeroconf = Zeroconf()
    try:
        zeroconf.register_service(info)
    except Exception as exc:  # noqa: BLE001
        print(f"[!] No se pudo registrar el servicio: {exc}", file=sys.stderr)
        zeroconf.close()
        return 1

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\n[*] Deteniendo el anuncio...")
    finally:
        try:
            zeroconf.unregister_service(info)
        finally:
            zeroconf.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
