package dev.syaroslavtsev.minibigo;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Показывает оценку сложности в конце строки с именем метода, например {@code O(n log n)}.
 *
 * @see ComplexityEstimator
 */
public class ComplexityInlayHintsProvider implements InlayHintsProvider {
    /** Добавляет подсказку для каждого метода */
    @Override
    public @Nullable InlayHintsCollector createCollector(@NotNull PsiFile psiFile, @NotNull Editor editor) {
        return (SharedBypassCollector) (element, sink) -> {
            if (!(element instanceof PsiMethod method)) {
                return;
            }

            String text = ComplexityEstimator.estimate(method).toString();
            // getTextOffset() у метода указывает на имя, а не на аннотации над ним
            int line = editor.getDocument().getLineNumber(method.getTextOffset());

            sink.addPresentation(
                    new EndOfLinePosition(line),
                    null,
                    "Asymptotic computational complexity",
                    HintFormat.Companion.getDefault(),
                    builder -> {
                        builder.text(text, null);
                        return Unit.INSTANCE;
                    }
            );
        };
    }
}
