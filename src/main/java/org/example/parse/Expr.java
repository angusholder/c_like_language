package org.example.parse;

import org.example.token.Token;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public sealed interface Expr {

    record Number(
            String text,
            Token token
    ) implements Expr {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(token, token);
        }
    }

    record Boolean(
            boolean value,
            Token token
    ) implements Expr {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(token, token);
        }
    }

    record Identifier(
            String text,
            Token token
    ) implements Expr {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(token, token);
        }
    }

    record Binary(
            Expr left,
            BinaryOp op,
            Expr right
    ) implements Expr {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(left.getTokenRange().start(), right.getTokenRange().end());
        }
    }

    enum BinaryOp {
        ADD,
        SUB,
        MUL,
        DIV,

        AND,
        OR,

        EQUALS,
        NOT_EQUALS,

        LT_EQ,
        LT,
        GT_EQ,
        GT,
    }

    record Unary(
            UnaryOp op,
            Expr expr,
            Token opToken
    ) implements Expr {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(opToken, expr.getTokenRange().end());
        }
    }

    enum UnaryOp {
        NOT,
        NEG,
    }

    record Call(
            String callee,
            List<Expr> arguments,
            Token calleeToken,
            Token closeParenToken
    ) implements Expr {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(calleeToken, closeParenToken);
        }
    }

    record Block(
            List<Expr> items,
            Token openBraceToken,
            Token closeBraceToken
    ) implements Expr {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(openBraceToken, closeBraceToken);
        }
    }

    record If(
            Expr condition,
            Expr.Block thenBranch,
            List<ElseIf> elseIfs,
            @Nullable
            Expr.Block elseBranch,
            Token ifToken
    ) implements Expr {
        @Override
        public TokenRange getTokenRange() {
            Token lastBraceToken;
            if (elseBranch != null) {
                lastBraceToken = elseBranch.getTokenRange().end();
            } else if (!elseIfs.isEmpty()) {
                lastBraceToken = elseIfs.get(elseIfs.size() - 1).thenBranch.getTokenRange().end();
            } else {
                lastBraceToken = thenBranch.getTokenRange().end();
            }
            return new TokenRange(ifToken, lastBraceToken);
        }
    }

    record ElseIf(
            Expr condition,
            Expr.Block thenBranch
    ) {}

    record While(
            Expr condition,
            Expr.Block body,
            Token whileToken
    ) implements Expr {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(whileToken, body.getTokenRange().end());
        }
    }

    record Function(
            Token nameToken,
            String name,
            @Nullable
            AstType returnType,
            List<FuncParam> parameters,
            Expr.Block body,
            Token funcToken
    ) implements Item {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(funcToken, body.getTokenRange().end());
        }
    }

    record FuncParam(
            String name,
            AstType type
    ) {}

    record Let(
            Token nameToken,
            String name,
            AstType type,
            Expr value,
            Token letToken
    ) implements Item {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(letToken, value.getTokenRange().end());
        }
    }

    /** The subset of expressions that are allowed at the top level in a file. */
    sealed interface Item extends Expr permits Function, Let {
    }

    record Assign(
            String lhs,
            Expr rhs,
            Token lhsToken
    ) implements Expr {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(lhsToken, rhs.getTokenRange().end());
        }
    }

    record Return(
            @Nullable
            Expr returnValue,
            Token returnToken
    ) implements Expr {
        @Override
        public TokenRange getTokenRange() {
            Token endToken;
            if (returnValue != null) {
                endToken = returnValue.getTokenRange().end();
            } else {
                endToken = returnToken;
            }
            return new TokenRange(returnToken, endToken);
        }
    }

    record TokenRange(
            Token start,
            Token end
    ) {
        public TokenRange {
            if (start.fileUid() != end.fileUid()) {
                throw new IllegalArgumentException("Tokens must be from the same file: " + start.fileUid() + ", " + end.fileUid());
            }
            if (start.startOffset() > end.endOffset()) {
                throw new IllegalArgumentException("Start token must be before end token: " + start.startOffset() + ", " + end.endOffset());
            }
        }

        public int fileUid() {
            return start.fileUid();
        }

        public int startOffset() {
            return start.startOffset();
        }

        public int endOffset() {
            return end.endOffset();
        }
    }

    TokenRange getTokenRange();
}
