#!/usr/bin/env python3
"""Upload a file to the application's upload API using a login session."""

import argparse
import getpass
import http.cookiejar
import json
import mimetypes
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path


def request(opener, url, *, data=None, headers=None):
    req = urllib.request.Request(url, data=data, headers=headers or {}, method="POST")
    try:
        with opener.open(req, timeout=300) as response:
            body = response.read()
            return json.loads(body) if body else None
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {exc.code}: {detail or exc.reason}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Не удалось подключиться: {exc.reason}") from exc


def upload_part(opener, base_url, upload_id, path, number, data, chunked):
    boundary = "----upload-" + uuid.uuid4().hex
    fields = {"id": str(upload_id), "isChunk": str(chunked).lower()}
    if chunked:
        fields["chunkId"] = str(number)

    # The server does not use the multipart filename; a fixed name avoids header injection.
    content_type = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
    pieces = []
    for name, value in fields.items():
        pieces.append(
            f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n'.encode()
        )
    pieces.append(
        f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="upload.bin"\r\n'
        f'Content-Type: {content_type}\r\n\r\n'.encode()
    )
    pieces.extend((data, f"\r\n--{boundary}--\r\n".encode()))
    request(opener, f"{base_url}/api/uploading", data=b"".join(pieces),
            headers={"Content-Type": f"multipart/form-data; boundary={boundary}"})


def upload_file(opener, base_url, path):
    if not path.is_file():
        raise RuntimeError(f"Файл не найден: {path}")
    size = path.stat().st_size
    if size == 0:
        raise RuntimeError("Пустые файлы сервер пока не принимает")

    params = urllib.parse.urlencode({"filename": path.name, "fileSize": size})
    plan = request(opener, f"{base_url}/api/uploading/open?{params}", data=b"")
    upload_id = plan["uploadId"]
    chunked = plan["chunked"]
    chunk_size = plan["chunkSize"] if chunked else size
    count = plan["chunkCount"] if chunked else 1
    if not isinstance(chunk_size, int) or chunk_size <= 0 or not isinstance(count, int) or count <= 0:
        raise RuntimeError(f"Сервер вернул некорректный план для загрузки {upload_id}")

    print(f"Загрузка {upload_id}: {size} байт, частей: {count}", flush=True)
    try:
        with path.open("rb") as source:
            for number in range(1, count + 1):
                data = source.read(chunk_size)
                expected = min(chunk_size, size - (number - 1) * chunk_size)
                if len(data) != expected:
                    raise RuntimeError("Файл изменился во время чтения")
                upload_part(opener, base_url, upload_id, path, number, data, chunked)
                print(f"Отправлена часть {number}/{count}: {len(data)} байт", flush=True)
            if source.read(1):
                raise RuntimeError("Файл увеличился во время загрузки")
        request(opener, f"{base_url}/api/uploading/{upload_id}/complete", data=b"")
    except (RuntimeError, OSError) as exc:
        raise RuntimeError(f"Загрузка {upload_id} не завершена: {exc}") from exc
    print(f"Загрузка {upload_id} завершена")


def main():
    parser = argparse.ArgumentParser(description="Загрузить файл в приложение через API.")
    parser.add_argument("file", nargs="?", type=Path, help="Путь к файлу; без него запускается интерактивный режим")
    parser.add_argument("--url", default="http://localhost:2721", help="Адрес backend")
    parser.add_argument("--username", help="Имя пользователя; если не указано, будет запрошено")
    args = parser.parse_args()

    base_url = args.url.rstrip("/")
    username = args.username or input("Логин: ").strip()
    if not username:
        raise RuntimeError("Логин не может быть пустым")
    password = getpass.getpass("Пароль: ")
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    login = json.dumps({"username": username, "password": password}).encode("utf-8")
    request(opener, f"{base_url}/api/auth/login", data=login, headers={"Content-Type": "application/json"})
    print("Вход выполнен.")

    if args.file is not None:
        upload_file(opener, base_url, args.file)
        return

    print("Вводите пути к файлам. Для выхода введите exit или нажмите Enter.")
    while True:
        try:
            value = input("Путь к файлу: ").strip()
        except EOFError:
            print()
            break
        if not value or value.lower() in {"exit", "quit"}:
            break
        if len(value) >= 2 and value[0] == value[-1] and value[0] in {'"', "'"}:
            value = value[1:-1]
        try:
            upload_file(opener, base_url, Path(value).expanduser())
        except (RuntimeError, OSError, ValueError, KeyError) as exc:
            print(f"Ошибка: {exc}", file=sys.stderr)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, ValueError, KeyError) as exc:
        print(f"Ошибка: {exc}", file=sys.stderr)
        sys.exit(1)
