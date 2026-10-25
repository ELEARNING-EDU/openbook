#!/usr/bin/env python3
"""Builds small, valid EPUB 3 books in several languages for testing the
voice picker's language filter. Run: python3 SampleBooks/make_books.py"""
import os
import zipfile

OUT = os.path.dirname(os.path.abspath(__file__))

BOOKS = [
    {
        "file": "01-english-the-quiet-harbour.epub",
        "lang": "en",
        "title": "The Quiet Harbour",
        "author": "A. Morgan",
        "chapters": [
            ("The Lighthouse", [
                "The harbour woke slowly. Fishing boats nudged against the wooden pier while gulls argued above the roofs, and the lighthouse at the end of the stone arm still turned, patient as an old man counting the hours.",
                "Mara walked down the hill with a loaf of bread under her arm and salt on her lips. She had lived in this town for sixteen years, long enough to know which doors stuck in the rain and which windows glowed first in the evening.",
                "At the end of the pier she stopped. The light above her swept the water, steady and unhurried, and for a moment the whole harbour seemed to hold its breath, waiting for something it could not name.",
            ]),
            ("The Letter", [
                "The letter arrived on a Tuesday, in an envelope softened by the journey. Mara read it twice on the doorstep, then a third time at the kitchen table, where the kettle sang and the cat slept and the world outside went quietly about its business.",
                "It was from her brother, who had left the harbour years ago and never written. He asked about the boats, about the lighthouse, about whether the bakery on the corner still opened before dawn. He did not ask to come home.",
            ]),
        ],
    },
    {
        "file": "02-french-le-port-tranquille.epub",
        "lang": "fr",
        "title": "Le Port Tranquille",
        "author": "Camille Rousseau",
        "chapters": [
            ("Le Phare", [
                "Le port s'éveillait lentement. Les barques de pêche se pressaient contre le quai de bois, les mouettes se disputaient au-dessus des toits, et le phare, au bout de la jetée, tournait encore, patient comme un vieil homme qui compte les heures.",
                "Camille descendit la colline avec un pain sous le bras et du sel sur les lèvres. Elle habitait cette ville depuis seize ans, assez longtemps pour savoir quelles portes résistaient à la pluie et quelles fenêtres s'allumaient les premières le soir.",
                "Au bout du quai, elle s'arrêta. La lumière au-dessus d'elle balayait l'eau, régulière et sans hâte, et pendant un instant le port tout entier sembla retenir son souffle.",
            ]),
            ("La Lettre", [
                "La lettre arriva un mardi, dans une enveloppe adoucie par le voyage. Camille la lut deux fois sur le seuil, puis une troisième fois dans la cuisine, où la bouilloire chantait, où le chat dormait, et où le monde continuait tranquillement son chemin.",
                "Elle venait de son frère, parti du port il y a des années sans jamais écrire. Il demandait des nouvelles des bateaux, du phare, et si la boulangerie du coin ouvrait toujours avant l'aube. Il ne demandait pas à revenir.",
            ]),
        ],
    },
    {
        "file": "03-spanish-el-puerto-tranquilo.epub",
        "lang": "es",
        "title": "El Puerto Tranquilo",
        "author": "Lucía Herrera",
        "chapters": [
            ("El Faro", [
                "El puerto despertaba despacio. Las barcas de pesca se acomodaban contra el muelle de madera, las gaviotas discutían sobre los tejados y el faro, al final del espigón, seguía girando, paciente como un anciano que cuenta las horas.",
                "Lucía bajó la colina con un pan bajo el brazo y sal en los labios. Vivía en aquel pueblo desde hacía dieciséis años, tiempo suficiente para saber qué puertas se atascaban con la lluvia y qué ventanas se encendían primero al anochecer.",
                "Al final del muelle se detuvo. La luz sobre ella barría el agua, constante y sin prisa, y por un instante todo el puerto pareció contener la respiración.",
            ]),
            ("La Carta", [
                "La carta llegó un martes, en un sobre ablandado por el viaje. Lucía la leyó dos veces en el umbral y una tercera en la cocina, donde la tetera cantaba, el gato dormía y el mundo seguía tranquilamente su camino.",
                "Era de su hermano, que había dejado el puerto años atrás sin escribir nunca. Preguntaba por los barcos, por el faro, por si la panadería de la esquina abría todavía antes del amanecer. No preguntaba si podía volver.",
            ]),
        ],
    },
    {
        "file": "04-japanese-shizukana-minato.epub",
        "lang": "ja",
        "title": "静かな港",
        "author": "佐藤 みどり",
        "chapters": [
            ("灯台", [
                "港はゆっくりと目を覚ました。漁船が木の桟橋に身を寄せ、カモメが屋根の上で言い争い、突堤の先の灯台は、時間を数える老人のように、変わらず回り続けていた。",
                "麻里はパンを脇に抱え、唇に潮の味を感じながら坂を下りた。この町に住んで十六年。雨の日にどの扉がきしむのか、夕暮れにどの窓が最初に灯るのか、もう知っていた。",
                "桟橋の突き当たりで彼女は立ち止まった。頭上の光が水面を静かに撫で、一瞬、港全体が息を止めたように思えた。",
            ]),
            ("手紙", [
                "手紙は火曜日に届いた。旅のあいだに柔らかくなった封筒だった。麻里は玄関で二度読み、台所でもう一度読んだ。やかんが鳴り、猫が眠り、外の世界は静かに自分の道を進んでいた。",
                "差出人は、何年も前に港を出て、一度も便りを寄こさなかった兄だった。船のこと、灯台のこと、角のパン屋が今も夜明け前に開くのかと尋ねていた。帰りたいとは書いていなかった。",
            ]),
        ],
    },
    {
        "file": "05-chinese-jijing-de-gangwan.epub",
        "lang": "zh",
        "title": "寂静的港湾",
        "author": "林 小雨",
        "chapters": [
            ("灯塔", [
                "港口慢慢醒来。渔船挨着木码头停靠，海鸥在屋顶上争吵，防波堤尽头的灯塔仍在转动，像一位数着时辰的老人，不慌不忙。",
                "玛拉腋下夹着一条面包走下山坡，嘴唇上带着咸味。她在这个小镇住了十六年，久到知道哪扇门会在雨天卡住，哪扇窗会在傍晚最先亮起。",
                "在码头尽头她停下脚步。头顶的灯光缓缓扫过水面，沉稳而不急迫，有那么一瞬间，整个港口仿佛都屏住了呼吸。",
            ]),
            ("来信", [
                "信在星期二送到，信封被旅途磨得柔软。玛拉在门口读了两遍，又在厨房读了一遍。水壶在响，猫在睡，外面的世界依旧安静地走着自己的路。",
                "信是哥哥写来的。他多年前离开港口，从此没有消息。他问起渔船，问起灯塔，问街角的面包店是否还在天亮前开门。他没有说要回来。",
            ]),
        ],
    },
]


def chapter_xhtml(title, paragraphs, lang):
    body = "\n".join(f"      <p>{p}</p>" for p in paragraphs)
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="{lang}" lang="{lang}">
  <head><title>{title}</title></head>
  <body>
    <h1>{title}</h1>
{body}
  </body>
</html>
"""


def build(book):
    path = os.path.join(OUT, book["file"])
    lang = book["lang"]
    chapters = book["chapters"]
    manifest = "\n".join(
        f'    <item id="ch{i+1}" href="ch{i+1}.xhtml" media-type="application/xhtml+xml"/>'
        for i in range(len(chapters))
    )
    spine = "\n".join(f'    <itemref idref="ch{i+1}"/>' for i in range(len(chapters)))
    nav_items = "\n".join(
        f'        <li><a href="ch{i+1}.xhtml">{title}</a></li>'
        for i, (title, _) in enumerate(chapters)
    )
    opf = f"""<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="bookid">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="bookid">urn:uuid:sample-{book["lang"]}-{abs(hash(book["file"])) % 100000}</dc:identifier>
    <dc:title>{book["title"]}</dc:title>
    <dc:creator>{book["author"]}</dc:creator>
    <dc:language>{lang}</dc:language>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
{manifest}
  </manifest>
  <spine>
{spine}
  </spine>
</package>
"""
    nav = f"""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="{lang}" lang="{lang}">
  <head><title>{book["title"]}</title></head>
  <body>
    <nav epub:type="toc" id="toc">
      <h1>{book["title"]}</h1>
      <ol>
{nav_items}
      </ol>
    </nav>
  </body>
</html>
"""
    container = """<?xml version="1.0" encoding="utf-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>
"""
    with zipfile.ZipFile(path, "w") as z:
        # The mimetype must be the first entry and stored, per the EPUB spec.
        z.writestr(zipfile.ZipInfo("mimetype"), "application/epub+zip", zipfile.ZIP_STORED)
        z.writestr("META-INF/container.xml", container, zipfile.ZIP_DEFLATED)
        z.writestr("OEBPS/content.opf", opf, zipfile.ZIP_DEFLATED)
        z.writestr("OEBPS/nav.xhtml", nav, zipfile.ZIP_DEFLATED)
        for i, (title, paragraphs) in enumerate(chapters):
            z.writestr(
                f"OEBPS/ch{i+1}.xhtml",
                chapter_xhtml(title, paragraphs, lang),
                zipfile.ZIP_DEFLATED,
            )
    print("wrote", path)


for book in BOOKS:
    build(book)
