package org.example.parse;

import org.example.token.Token;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public sealed interface AstExpr {

    record Number(
            String text,
            Token token
    ) implements AstExpr {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(token, token);
        }
    }

    record Boolean(
            boolean value,
            Token token
    ) implements AstExpr {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(token, token);
        }
    }

    record Identifier(
            String text,
            Token token
    ) implements AstExpr {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(token, token);
        }
    }

    record Binary(
            AstExpr left,
            BinaryOp op,
            AstExpr right
    ) implements AstExpr {
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
            AstExpr expr,
            Token opToken
    ) implements AstExpr {
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
            List<AstExpr> arguments,
            Token calleeToken,
            Token closeParenToken
    ) implements AstExpr {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(calleeToken, closeParenToken);
        }
    }

    record Block(
            List<AstExpr> items,
            Token openBraceToken,
            Token closeBraceToken
    ) implements AstExpr {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(openBraceToken, closeBraceToken);
        }
    }

    record If(
            AstExpr condition,
            AstExpr.Block thenBranch,
            List<ElseIf> elseIfs,
            @Nullable
            AstExpr.Block elseBranch,
            Token ifToken
    ) implements AstExpr {
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
            AstExpr condition,
            AstExpr.Block thenBranch
    ) {}

    record While(
            AstExpr condition,
            AstExpr.Block body,
            Token whileToken
    ) implements AstExpr {
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
            AstExpr.Block body,
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
            AstExpr value,
            Token letToken
    ) implements Item {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(letToken, value.getTokenRange().end());
        }
    }

    /** The subset of expressions that are allowed at the top level in a file. */
    sealed interface Item extends AstExpr permits Function, Let {
    }

    record Assign(
            String lhs,
            AstExpr rhs,
            Token lhsToken
    ) implements AstExpr {
        @Override
        public TokenRange getTokenRange() {
            return new TokenRange(lhsToken, rhs.getTokenRange().end());
        }
    }

    record Return(
            @Nullable
            AstExpr returnValue,
            Token returnToken
    ) implements AstExpr {
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
