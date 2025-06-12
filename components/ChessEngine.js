import { useEffect, useRef } from 'react';

class ChessEngine {
  constructor() {
    this.engine = null;
    this.isReady = false;
  }

  async init() {
    if (typeof window !== 'undefined') {
      const { default: Stockfish } = await import('stockfish');
      this.engine = new Stockfish();
      
      return new Promise((resolve) => {
        this.engine.onmessage = (e) => {
          if (e.data === 'uciok') {
            this.isReady = true;
            resolve();
          }
        };
        this.engine.postMessage('uci');
        this.engine.postMessage('isready');
      });
    }
  }

  setDifficulty(level) {
    if (this.engine && this.isReady) {
      // Устанавливаем уровень сложности (1-20)
      const skillLevel = Math.floor((level / 20) * 20);
      this.engine.postMessage(`setoption name Skill Level value ${skillLevel}`);
      this.engine.postMessage(`setoption name Skill Level Maximum Error value ${20 - level}`);
      this.engine.postMessage(`setoption name Skill Level Probability value ${20 - level}`);
    }
  }

  async getBestMove(fen, timeLimit = 1000) {
    if (!this.engine || !this.isReady) {
      throw new Error('Движок не инициализирован');
    }

    return new Promise((resolve) => {
      let bestMove = null;

      this.engine.onmessage = (e) => {
        const message = e.data;
        if (message.startsWith('bestmove')) {
          bestMove = message.split(' ')[1];
          resolve(bestMove);
        }
      };

      this.engine.postMessage(`position fen ${fen}`);
      this.engine.postMessage(`go movetime ${timeLimit}`);
    });
  }

  stop() {
    if (this.engine && this.isReady) {
      this.engine.postMessage('stop');
    }
  }

  quit() {
    if (this.engine && this.isReady) {
      this.engine.postMessage('quit');
      this.engine = null;
      this.isReady = false;
    }
  }
}

export default ChessEngine; 