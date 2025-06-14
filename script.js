const boardEl = document.getElementById('board');
const scoreEl = document.getElementById('score');
const movesEl = document.getElementById('moves');
const newGameBtn = document.getElementById('newGame');

const size = 8; // default board size
const tileTypes = ['\u25CF', '\u25A0', '\u25B2', '\u25C6', '\u25BC']; // simple shapes
let board = [];
let score = 0;
let moves = 0;
let firstSelection = null;

function init() {
  boardEl.style.gridTemplateColumns = `repeat(${size}, 40px)`;
  board = [];
  score = 0;
  moves = 0;
  for (let r = 0; r < size; r++) {
    const row = [];
    for (let c = 0; c < size; c++) {
      row.push(randomTile());
    }
    board.push(row);
  }
  render();
  updateInfo();
}

function randomTile() {
  return tileTypes[Math.floor(Math.random() * tileTypes.length)];
}

function render() {
  boardEl.innerHTML = '';
  board.forEach((row, r) => {
    row.forEach((tile, c) => {
      const div = document.createElement('div');
      div.className = 'cell';
      div.textContent = tile;
      div.addEventListener('click', () => select(r, c, div));
      if (firstSelection && firstSelection.r === r && firstSelection.c === c) {
        div.classList.add('selected');
      }
      boardEl.appendChild(div);
    });
  });
}

function updateInfo() {
  scoreEl.textContent = score;
  movesEl.textContent = moves;
}

function select(r, c, el) {
  if (!firstSelection) {
    firstSelection = { r, c };
    el.classList.add('selected');
  } else {
    swap(firstSelection, { r, c });
    firstSelection = null;
    moves++;
    collapseMatches();
    render();
    updateInfo();
  }
}

function swap(a, b) {
  const temp = board[a.r][a.c];
  board[a.r][a.c] = board[b.r][b.c];
  board[b.r][b.c] = temp;
}

function collapseMatches() {
  let found = false;
  const remove = Array.from({ length: size }, () => Array(size).fill(false));

  // horizontal
  for (let r = 0; r < size; r++) {
    for (let c = 0; c < size - 2; c++) {
      const t = board[r][c];
      if (t && board[r][c + 1] === t && board[r][c + 2] === t) {
        remove[r][c] = remove[r][c + 1] = remove[r][c + 2] = true;
      }
    }
  }
  // vertical
  for (let c = 0; c < size; c++) {
    for (let r = 0; r < size - 2; r++) {
      const t = board[r][c];
      if (t && board[r + 1][c] === t && board[r + 2][c] === t) {
        remove[r][c] = remove[r + 1][c] = remove[r + 2][c] = true;
      }
    }
  }

  for (let r = 0; r < size; r++) {
    for (let c = 0; c < size; c++) {
      if (remove[r][c]) {
        board[r][c] = null;
        score += 10;
        found = true;
      }
    }
  }

  if (found) {
    dropTiles();
    collapseMatches();
  }
}

function dropTiles() {
  for (let c = 0; c < size; c++) {
    for (let r = size - 1; r >= 0; r--) {
      if (board[r][c] === null) {
        for (let k = r; k > 0; k--) {
          board[k][c] = board[k - 1][c];
        }
        board[0][c] = randomTile();
      }
    }
  }
}

newGameBtn.addEventListener('click', init);
init();
