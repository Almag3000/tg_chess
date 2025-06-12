declare module 'chessboardjsx' {
  import { Component } from 'react';

  interface ChessboardProps {
    position: string;
    onPieceDrop?: (move: { sourceSquare: string; targetSquare: string }) => boolean;
    boardWidth?: number;
    arePiecesDraggable?: boolean;
    customBoardStyle?: React.CSSProperties;
    customDarkSquareStyle?: React.CSSProperties;
    customLightSquareStyle?: React.CSSProperties;
  }

  const Chessboard: React.FC<ChessboardProps>;
  export default Chessboard;
} 