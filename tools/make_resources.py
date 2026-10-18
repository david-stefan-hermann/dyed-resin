#!/usr/bin/env python3
"""Blockstates, models, item definitions, loot tables, recipes, tags and names of the 16 dyed resin clumps.

    python3 tools/make_resources.py

Everything is copied from vanilla's resin clump with the colour put in. The names go into the language files next
to the keys written by hand (the guide book's texts), which stay as they are.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/dyedresin'
DATA = RES / 'data/dyedresin'

# colour: (English, German); German "Harzklumpen" is masculine
COLOURS = {
    'white': ('White', 'Weißer'),
    'orange': ('Orange', 'Oranger'),
    'magenta': ('Magenta', 'Magenta'),
    'light_blue': ('Light Blue', 'Hellblauer'),
    'yellow': ('Yellow', 'Gelber'),
    'lime': ('Lime', 'Hellgrüner'),
    'pink': ('Pink', 'Rosa'),
    'gray': ('Gray', 'Grauer'),
    'light_gray': ('Light Gray', 'Hellgrauer'),
    'cyan': ('Cyan', 'Türkiser'),
    'purple': ('Purple', 'Violetter'),
    'blue': ('Blue', 'Blauer'),
    'brown': ('Brown', 'Brauner'),
    'green': ('Green', 'Grüner'),
    'red': ('Red', 'Roter'),
    'black': ('Black', 'Schwarzer'),
}
FACES = ('down', 'up', 'north', 'south', 'west', 'east')
# vanilla's multipart: each face's rotation; a clump with no face set shows all six (as in vanilla)
ROTATIONS = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8', newline='\n')


def blockstate(name):
    parts = []
    for face, rotation in ROTATIONS.items():
        model = {'model': 'dyedresin:block/' + name, **rotation}
        if rotation:
            model['uvlock'] = True
        parts.append({'apply': model, 'when': {face: 'true'}})
        parts.append({'apply': model, 'when': {f: 'false' for f in FACES}})
    return {'multipart': parts}


def loot(name):
    counts = [{'add': True, 'conditions': [{'block': 'dyedresin:' + name, 'condition': 'minecraft:block_state_property',
                                            'properties': {face: 'true'}}],
               'count': 1.0, 'function': 'minecraft:set_count'} for face in FACES]
    return {'type': 'minecraft:block', 'pools': [{'entries': [{
        'type': 'minecraft:item',
        'functions': counts + [{'add': True, 'count': -1.0, 'function': 'minecraft:set_count'},
                               {'function': 'minecraft:explosion_decay'}],
        'name': 'dyedresin:' + name}], 'rolls': 1.0}], 'random_sequence': 'dyedresin:blocks/' + name}


def main():
    names = {'en_us': {}, 'de_de': {}}
    for colour, (english, german) in COLOURS.items():
        name = colour + '_resin_clump'
        write(ASSETS / 'blockstates' / (name + '.json'), blockstate(name))
        write(ASSETS / 'models/block' / (name + '.json'), {
            'parent': 'minecraft:block/resin_clump',
            'textures': {'particle': 'dyedresin:block/' + name, 'texture': 'dyedresin:block/' + name}})
        write(ASSETS / 'models/item' / (name + '.json'), {
            'parent': 'minecraft:item/generated', 'textures': {'layer0': 'dyedresin:item/' + name}})
        write(ASSETS / 'items' / (name + '.json'), {'model': {'type': 'minecraft:model', 'model': 'dyedresin:item/' + name}})
        write(DATA / 'loot_table/blocks' / (name + '.json'), loot(name))
        # one clump and a dye; any clump, dyed or not, so a dyed one can be dyed again
        write(DATA / 'recipe' / (name + '.json'), {
            'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'group': 'dyed_resin_clump',
            'ingredients': ['#dyedresin:resin_clumps', 'minecraft:%s_dye' % colour],
            'result': {'id': 'dyedresin:' + name}})
        # eight around a dye, as with terracotta
        write(DATA / 'recipe' / (name + '_from_eight.json'), {
            'type': 'minecraft:crafting_shaped', 'category': 'misc', 'group': 'dyed_resin_clump_eight',
            'key': {'#': '#dyedresin:resin_clumps', 'X': 'minecraft:%s_dye' % colour},
            'pattern': ['###', '#X#', '###'],
            'result': {'count': 8, 'id': 'dyedresin:' + name}})
        names['en_us']['block.dyedresin.' + name] = english + ' Resin Clump'
        names['de_de']['block.dyedresin.' + name] = german + ' Harzklumpen'
    dyed = ['dyedresin:%s_resin_clump' % c for c in COLOURS]
    write(DATA / 'tags/item/dyed_resin_clumps.json', {'values': dyed})
    write(DATA / 'tags/item/resin_clumps.json', {'values': ['minecraft:resin_clump', '#dyedresin:dyed_resin_clumps']})
    # a dyed clump alone in the grid washes out to a plain one
    write(DATA / 'recipe/resin_clump_from_dyed.json', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': ['#dyedresin:dyed_resin_clumps'], 'result': {'id': 'minecraft:resin_clump'}})
    recipes = ['dyedresin:%s_resin_clump%s' % (c, s) for c in COLOURS for s in ('', '_from_eight')]
    recipes += ['dyedresin:resin_clump_from_dyed', 'dyedresin:guide_book']  # the book's recipe takes a resin clump too
    write(DATA / 'advancement/recipes/misc/dyed_resin_clumps.json', {
        'parent': 'minecraft:recipes/root',
        'criteria': {'has_resin_clump': {'conditions': {'items': [{'items': '#dyedresin:resin_clumps'}]},
                                         'trigger': 'minecraft:inventory_changed'}},
        'requirements': [['has_resin_clump']],
        'rewards': {'recipes': recipes}})
    # the block tags vanilla's clump is in (no mineable tag: any tool or none breaks it at once)
    blocks = {'values': dyed}
    write(RES / 'data/minecraft/tags/block/replaceable.json', blocks)
    write(RES / 'data/minecraft/tags/block/combination_step_sound_blocks.json', blocks)
    for language, entries in names.items():
        path = ASSETS / 'lang' / (language + '.json')
        lang = json.loads(path.read_text(encoding='utf-8'))
        lang.update(entries)
        write(path, lang)
    print('%d colours, %d recipes' % (len(COLOURS), len(recipes)))


if __name__ == '__main__':
    main()
