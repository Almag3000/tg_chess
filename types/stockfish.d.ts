declare module 'stockfish' {
  class Stockfish {
    constructor();
    onmessage: ((e: { data: string }) => void) | null;
    postMessage(message: string): void;
  }
  export default Stockfish;
} 