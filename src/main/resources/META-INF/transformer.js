function initializeCoreMod() {
    return {
        'coreModName': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.gui.screen.Screen',
                'methodName': 'renderBackground', //not obfuscated
                'methodDesc': '(I)V'
            },
            'transformer': function(methodNode) {

                var DRAW_WORLD_BAGKGROUND_METHOD = "drawWorldBackground";
                var DRAW_WORLD_BAGKGROUND_METHOD_OBF = "func_146270_b";

                var BLUR_MAIN_CLASS = "com/tterrag/blur/Blur";
                var COLOR_HOOK_METHOD_NAME = "getBackgroundColor";
                var COLOR_HOOK_METHOD_DESC = "(Z)I";

                var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
                var Opcodes = Java.type('org.objectweb.asm.Opcodes');
                var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
                //build the new insn list
                var colorHook = ASMAPI.buildMethodCall(BLUR_MAIN_CLASS, COLOR_HOOK_METHOD_NAME, COLOR_HOOK_METHOD_DESC, ASMAPI.MethodType.STATIC);
                var colorHook2 = colorHook.clone(null)
                var newList = ASMAPI.listOf(
                    new InsnNode(Opcodes.ICONST_1),
                    colorHook,
                    new InsnNode(Opcodes.ICONST_0),
                    colorHook2
                );
                var invokeNode = ASMAPI.findFirstMethodCall(methodNode, ASMAPI.MethodType.VIRTUAL, "net/minecraft/client/gui/screen/Screen", "fillGradient", "(IIIIII)V");
                //Get the nodes to replace and remove them
                var toRemove1 = invokeNode.getPrevious();
                if (toRemove1.getOpcode() != Opcodes.LDC)
                    throw "toRemove 1: Invalid Opcode " + toRemove1.getOpcode();
                var toRemove2 = toRemove1.getPrevious();
                if (toRemove2.getOpcode() != Opcodes.LDC)
                    throw "toRemove 2: Invalid Opcode " + toRemove2.getOpcode();
                methodNode.instructions.remove(toRemove1);
                methodNode.instructions.remove(toRemove2);
                //Insert the new insn list where the LDC nodes where previously
                methodNode.instructions.insertBefore(invokeNode, newList);
                return methodNode;
            }
        }
    }
}