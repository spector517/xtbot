package com.github.spector517.xtbot.core.loader.asm;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

@Accessors(fluent = true)
class AnnotationScanner extends ClassVisitor {

    private final String annotationDescriptor;

    @Getter
    private boolean found;

    public AnnotationScanner(String annotationDescriptor) {
        super(Opcodes.ASM9);
        this.annotationDescriptor = annotationDescriptor;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.equals(annotationDescriptor)) {
            found = true;
        }
        return super.visitAnnotation(desc, visible);
    }
}
