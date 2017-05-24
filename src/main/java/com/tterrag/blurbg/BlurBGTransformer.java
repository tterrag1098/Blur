package com.tterrag.blurbg;

import java.util.Iterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.launchwrapper.IClassTransformer;

public class BlurBGTransformer implements IClassTransformer {

    private static final String GUI_SCREEN_CLASS_NAME = "net.minecraft.client.gui.GuiScreen";
    
    private static final String DRAW_WORLD_BAGKGROUND_METHOD = "drawWorldBackground";
    private static final String DRAW_WORLD_BAGKGROUND_METHOD_OBF = "func_146270_b";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformedName.equals(GUI_SCREEN_CLASS_NAME)) {
            System.out.println("Transforming Class [" + transformedName + "], Method [" + DRAW_WORLD_BAGKGROUND_METHOD + "]");

            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);

            Iterator<MethodNode> methods = classNode.methods.iterator();
            
            while (methods.hasNext()) {
                MethodNode m = methods.next();
                if (m.name.equals(DRAW_WORLD_BAGKGROUND_METHOD) || m.name.equals(DRAW_WORLD_BAGKGROUND_METHOD_OBF)) {
                    for (int i = 0; i < m.instructions.size(); i++) {
                        AbstractInsnNode next = m.instructions.get(i);
                        
//                        if (next.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode)next).name.equals(DRAW_GRADIENT_RECT_METHOD_NAME)) {
//                            while (!(next instanceof LabelNode)) {
//                                m.instructions.remove(next);
//                                next = m.instructions.get(--i);
//                            }
//                            break;
//                        }
                        if (next.getOpcode() == Opcodes.LDC) {
                            // TODO make this configurable? 
                            System.out.println("Modifying GUI background darkness... ");
                            ((LdcInsnNode)next).cst = ((LdcInsnNode)next.getNext()).cst = 0x66000000;
                            break;
                        }
                    }
                    break;
                }
            }
            
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(cw);
            System.out.println("Transforming " + transformedName + " Finished.");
            return cw.toByteArray();
        }
    
        return basicClass;
    }

}
