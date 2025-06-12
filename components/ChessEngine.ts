class ChessEngine {
  private engine: any;
  private isReady: boolean;

  constructor() {
    this.engine = null;
    this.isReady = false;
  }

  async init(): Promise<void> {
    if (typeof window !== 'undefined') {
      const { default: Stockfish } = await import('stockfish');
      this.engine = new Stockfish();
      
      return new Promise((resolve) => {
        this.engine.onmessage = (e: any) => {
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

  setDifficulty(level: number): void {
    if (this.engine && this.isReady) {
      const skillLevel = Math.floor((level / 20) * 20);
      this.engine.postMessage(`setoption name Skill Level value ${skillLevel}`);
      this.engine.postMessage(`setoption name Skill Level Maximum Error value ${20 - level}`);
      this.engine.postMessage(`setoption name Skill Level Probability value ${20 - level}`);
    }
  }

  async getBestMove(fen: string, timeLimit: number = 1000): Promise<string> {
    if (!this.engine || !this.isReady) {
      throw new Error('Движок не инициализирован');
    }

    return new Promise((resolve) => {
      let bestMove: string | null = null;

      this.engine.onmessage = (e: any) => {
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

  stop(): void {
    if (this.engine && this.isReady) {
      this.engine.postMessage('stop');
    }
  }

  quit(): void {
    if (this.engine && this.isReady) {
      this.engine.postMessage('quit');
      this.engine = null;
      this.isReady = false;
    }
  }
}

export default ChessEngine; 