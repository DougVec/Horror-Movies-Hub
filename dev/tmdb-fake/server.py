"""
TMDB falsa para testar o Horror Movie Hub sem token da API.
Imita só as rotas que a aplicação usa: /search/movie, /discover/movie, /movie/{id} e /movie/{id}/watch/providers.
Catálogo fixo de demonstração; estreias "em breve" são calculadas a partir de hoje.
Só usa a biblioteca padrão do Python.
"""
import json
import unicodedata
from datetime import date, timedelta
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

HORROR, COMEDIA = 27, 35
hoje = date.today()

# id, título pt-BR, título original, ano/data, minutos, nota, gêneros, streaming no BR
FILMES = [
    (694, "O Iluminado", "The Shining", "1980-05-23", 146, 8.2, [HORROR], ["Max"]),
    (493922, "Hereditário", "Hereditary", "2018-06-07", 127, 7.3, [HORROR], ["Prime Video"]),
    (138843, "Invocação do Mal", "The Conjuring", "2013-07-18", 112, 7.5, [HORROR], ["Max", "Netflix"]),
    (419430, "Corra!", "Get Out", "2017-02-24", 104, 7.6, [HORROR], []),
    (9552, "O Exorcista", "The Exorcist", "1973-12-26", 122, 7.7, [HORROR], ["Max"]),
    (948, "Halloween: A Noite do Terror", "Halloween", "1978-10-25", 91, 7.5, [HORROR], []),
    (530385, "Midsommar: O Mal Não Espera a Noite", "Midsommar", "2019-07-03", 148, 7.1, [HORROR], ["Prime Video"]),
    (346364, "It: A Coisa", "It", "2017-09-06", 135, 7.2, [HORROR], ["Netflix"]),
    (447332, "Um Lugar Silencioso", "A Quiet Place", "2018-04-03", 90, 7.4, [HORROR], ["Paramount+"]),
    (4232, "Pânico", "Scream", "1996-12-20", 111, 7.4, [HORROR], []),
    (900001, "A Casa do Fim da Rua (demo)", "House at the End (demo)", str(hoje + timedelta(days=16)), 98, 0, [HORROR], []),
    (900002, "Sussurros no Porão (demo)", "Basement Whispers (demo)", str(hoje + timedelta(days=36)), 104, 0, [HORROR], []),
    # não é terror: a busca do hub precisa filtrar
    (8363, "Superbad: É Hoje", "Superbad", "2007-08-17", 113, 7.2, [COMEDIA], []),
]


def sem_acento(s):
    return "".join(c for c in unicodedata.normalize("NFD", s.lower()) if unicodedata.category(c) != "Mn")


def resumo(f):
    id_, titulo, original, data, _, nota, generos, _ = f
    return {"id": id_, "title": titulo, "original_title": original, "overview": f"Sinopse de demonstração de {titulo}.",
            "release_date": data, "poster_path": None, "vote_average": nota, "genre_ids": generos}


def detalhe(f):
    d = resumo(f)
    del d["genre_ids"]
    d["runtime"] = f[4]
    d["genres"] = [{"id": g} for g in f[6]]
    return d


class Handler(BaseHTTPRequestHandler):
    def responder(self, status, corpo):
        dados = json.dumps(corpo).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(dados)))
        self.end_headers()
        self.wfile.write(dados)

    def do_GET(self):
        url = urlparse(self.path)
        qs = parse_qs(url.query)
        partes = url.path.strip("/").split("/")  # ["3", "movie", "694", ...]

        if url.path == "/3/search/movie":
            q = sem_acento(qs.get("query", [""])[0])
            achados = [resumo(f) for f in FILMES if q in sem_acento(f[1]) or q in sem_acento(f[2])]
            return self.responder(200, {"results": achados})

        if url.path == "/3/discover/movie":
            desde = qs.get("primary_release_date.gte", [str(hoje)])[0]
            futuros = sorted((f for f in FILMES if HORROR in f[6] and f[3] >= desde), key=lambda f: f[3])
            return self.responder(200, {"results": [resumo(f) for f in futuros]})

        if len(partes) >= 3 and partes[1] == "movie" and partes[2].isdigit():
            filme = next((f for f in FILMES if f[0] == int(partes[2])), None)
            if filme is None:
                return self.responder(404, {"status_message": "The resource you requested could not be found."})
            if len(partes) == 3:
                return self.responder(200, detalhe(filme))
            if partes[3:] == ["watch", "providers"]:
                br = {"flatrate": [{"provider_name": p} for p in filme[7]]} if filme[7] else {}
                return self.responder(200, {"id": filme[0], "results": {"BR": br} if br else {}})

        self.responder(404, {"status_message": "rota não simulada"})

    def log_message(self, fmt, *args):
        print("tmdb-fake:", self.path, flush=True)


if __name__ == "__main__":
    print("TMDB falsa em http://0.0.0.0:8099/3", flush=True)
    ThreadingHTTPServer(("0.0.0.0", 8099), Handler).serve_forever()
