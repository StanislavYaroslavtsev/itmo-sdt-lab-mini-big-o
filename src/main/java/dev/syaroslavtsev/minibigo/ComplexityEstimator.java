package dev.syaroslavtsev.minibigo;

import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Оценивает сложность метода по вложенности циклов.
 */
public final class ComplexityEstimator extends JavaRecursiveElementVisitor {
    /** Стек оценок для вложенных циклов. */
    private final Deque<Complexity> stack = new ArrayDeque<>();
    /** Самая большая оценка на данный момент. */
    private Complexity result = Complexity.constant();

    private ComplexityEstimator() {
        stack.push(Complexity.constant());
    }

    /**
     * Оценивает сложность метода.
     *
     * @param method метод
     * @return оценка; для метода без тела — {@code O(1)}
     */
    public static Complexity estimate(PsiMethod method) {
        ComplexityEstimator estimator = new ComplexityEstimator();
        PsiCodeBlock body = method.getBody();
        if (body != null) {
            body.accept(estimator);
        }
        return estimator.result;
    }

    /** for: константные границы — O(1), счётчик умножается/делится — O(log n), иначе O(n). */
    @Override
    public void visitForStatement(@NotNull PsiForStatement loop) {
        Complexity iterations;
        if (hasConstantBounds(loop)) {
            iterations = Complexity.constant();
        } else if (hasMultiplicativeAssignment(loop.getUpdate())) {
            iterations = Complexity.logarithmic();
        } else {
            iterations = Complexity.linear();
        }
        enterLoop(loop, iterations);
    }

    /** for-each всегда O(n). */
    @Override
    public void visitForeachStatement(@NotNull PsiForeachStatement loop) {
        enterLoop(loop, Complexity.linear());
    }

    /** while, см. {@link #whileIterations}. */
    @Override
    public void visitWhileStatement(@NotNull PsiWhileStatement loop) {
        enterLoop(loop, whileIterations(loop.getCondition(), loop.getBody()));
    }

    /** do-while, см. {@link #whileIterations}. */
    @Override
    public void visitDoWhileStatement(@NotNull PsiDoWhileStatement loop) {
        enterLoop(loop, whileIterations(loop.getCondition(), loop.getBody()));
    }

    @Override
    public void visitLambdaExpression(@NotNull PsiLambdaExpression expression) {}

    @Override
    public void visitClass(@NotNull PsiClass aClass) {}

    /**
     * Заходит в цикл: кладёт оценку на стек, обходит тело и снимает её.
     *
     * @param loop       цикл
     * @param iterations сколько итераций делает цикл
     */
    private void enterLoop(PsiLoopStatement loop, Complexity iterations) {
        Complexity current = stack.peek().times(iterations);
        stack.push(current);
        result = result.max(current);
        try {
            PsiStatement body = loop.getBody();
            if (body != null) {
                body.accept(this);
            }
        } finally {
            stack.pop();
        }
    }

    /**
     * Итерации while и do-while: если переменная из условия делится или умножается в теле
     * ({@code while (n > 0) n /= 2}) — O(log n), иначе O(n).
     */
    private static Complexity whileIterations(PsiExpression condition, PsiStatement body) {
        if (condition != null && body != null) {
            for (PsiAssignmentExpression a : PsiTreeUtil.findChildrenOfType(body, PsiAssignmentExpression.class)) {
                if (isMultiplicative(a)
                        && a.getLExpression() instanceof PsiReferenceExpression ref
                        && ref.resolve() instanceof PsiVariable v
                        && mentions(condition, v)) {
                    return Complexity.logarithmic();
                }
            }
        }
        return Complexity.linear();
    }

    /** Цикл с константными границами, например {@code for (int i = 0; i < 10; i++)}. */
    private static boolean hasConstantBounds(PsiForStatement loop) {
        // начало: int i = 0
        boolean constantStart = loop.getInitialization() instanceof PsiDeclarationStatement decl
                && decl.getDeclaredElements().length > 0
                && decl.getDeclaredElements()[0] instanceof PsiLocalVariable v
                && isConstant(v.getInitializer());
        // конец: i < 10
        return constantStart
                && loop.getCondition() instanceof PsiBinaryExpression b
                && (isConstant(b.getLOperand()) || isConstant(b.getROperand()));
    }

    /** Значение известно при компиляции: {@code 10}, {@code 2 * 5}, {@code static final MAX}. */
    private static boolean isConstant(PsiExpression e) {
        return e != null && JavaPsiFacade.getInstance(e.getProject())
                .getConstantEvaluationHelper()
                .computeConstantExpression(e) != null;
    }

    /** Есть ли внутри умножение/деление: {@code i *= 2}, {@code i /= 2}, {@code i = i * 2}. */
    private static boolean hasMultiplicativeAssignment(PsiElement element) {
        if (element == null) {
            return false;
        }

        for (PsiAssignmentExpression a : PsiTreeUtil.findChildrenOfType(element, PsiAssignmentExpression.class)) {
            if (isMultiplicative(a)) {
                return true;
            }
        }

        return false;
    }

    /** Присваивание умножает или делит: {@code x *= c}, {@code x /= c}, {@code x = x * c}, {@code x = x / c}. */
    private static boolean isMultiplicative(PsiAssignmentExpression a) {
        IElementType op = a.getOperationTokenType();
        // x *= c, x /= c
        if (op == JavaTokenType.ASTERISKEQ || op == JavaTokenType.DIVEQ) {
            return true;
        }
        // x = x * c, x = x / c
        return op == JavaTokenType.EQ
                && a.getRExpression() instanceof PsiBinaryExpression b
                && (b.getOperationTokenType() == JavaTokenType.ASTERISK
                || b.getOperationTokenType() == JavaTokenType.DIV);
    }

    /** Встречается ли переменная {@code v} внутри {@code root}. */
    private static boolean mentions(PsiElement root, PsiVariable v) {
        if (root instanceof PsiReferenceExpression self && v.equals(self.resolve())) {
            return true;
        }
        for (PsiReferenceExpression ref : PsiTreeUtil.findChildrenOfType(root, PsiReferenceExpression.class)) {
            if (v.equals(ref.resolve())) {
                return true;
            }
        }
        return false;
    }
}
