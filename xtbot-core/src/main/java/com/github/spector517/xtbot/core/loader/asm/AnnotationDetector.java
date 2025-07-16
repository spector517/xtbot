package com.github.spector517.xtbot.core.loader.asm;

import lombok.experimental.UtilityClass;
import org.objectweb.asm.ClassReader;

import java.lang.annotation.Annotation;

@UtilityClass
public class AnnotationDetector {

    public boolean isAnnotationPresentInClass(byte[] classBytes, Class<? extends Annotation> annotation) {
        var annotationDescriptor = "L%s;".formatted(annotation.getName().replaceAll("\\.", "/"));
        var visitor = new AnnotationScanner(annotationDescriptor);
        var reader = new ClassReader(classBytes);
        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return visitor.found();
    }
}
