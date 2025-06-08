import { Game } from 'js-chess-engine';
import assert from 'assert';

const game = new Game();
assert.ok(typeof game.exportFEN() === 'string');
console.log('ok');
