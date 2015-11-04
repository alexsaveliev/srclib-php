package com.sourcegraph.toolchain.php;

import com.sourcegraph.toolchain.core.Def;
import com.sourcegraph.toolchain.core.DefKey;
import com.sourcegraph.toolchain.core.Ref;
import com.sourcegraph.toolchain.php.antlr4.PHPParser;
import com.sourcegraph.toolchain.php.antlr4.PHPParserBaseListener;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileGrapher extends PHPParserBaseListener implements ANTLRErrorListener {

    private static Logger LOGGER = LoggerFactory.getLogger(FileGrapher.class);

    private static final Pattern INCLUDE_EXPRESSION = Pattern.compile("\\s*\\(?\\s*['\"]([^'\"]+)['\"]\\s*\\)?");

    private PHPGraph graph;

    private String file;

    private String blockName;
    private ClassInfo currentClassInfo;

    public FileGrapher(PHPGraph graph, String file) {
        this.graph = graph;
        this.file = file;

        graph.vars.push(new HashMap<>());
    }

    protected Def def(ParserRuleContext ctx, String kind) {
        Def def = new Def();
        def.defStart = ctx.getStart().getStartIndex();
        def.defEnd = ctx.getStop().getStopIndex();
        def.name = ctx.getText();
        def.file = this.file;
        def.kind = kind;
        initDef(def);
        return def;
    }

    protected Def def(Token token, String kind) {
        Def def = new Def();
        def.defStart = token.getStartIndex();
        def.defEnd = token.getStopIndex();
        def.name = token.getText();
        def.file = this.file;
        def.kind = kind;
        initDef(def);
        return def;
    }

    protected void initDef(Def def) {
        def.test = false; // not in PHP
        def.exported = DefKind.FUNCTION.equals(def.kind); // TODO
        def.local = !def.exported;
    }

    protected Ref ref(ParserRuleContext ctx) {
        Ref ref = new Ref();
        ref.start = ctx.getStart().getStartIndex();
        ref.end = ctx.getStop().getStopIndex();
        ref.file = this.file;
        return ref;
    }

    protected Ref ref(TerminalNode node) {
        Ref ref = new Ref();
        ref.start = node.getSymbol().getStartIndex();
        ref.end = node.getSymbol().getStopIndex();
        ref.file = this.file;
        return ref;
    }

    protected void emit(Def def) {
        try {
            graph.writer.writeDef(def);
        } catch (IOException e) {
            e.printStackTrace(); // TODO
        }
        // auto-adding self-references
        Ref ref = new Ref();
        ref.defKey = def.defKey;
        ref.def = true;
        ref.start = def.defStart;
        ref.end = def.defEnd;
        ref.file = def.file;
        emit(ref);
    }

    protected void emit(Ref ref) {
        try {
            graph.writer.writeRef(ref);
        } catch (IOException e) {
            e.printStackTrace(); // TODO
        }
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        LOGGER.warn("{} at {}:{}: {}", file, line, charPositionInLine, msg);
    }

    @Override
    public void reportAmbiguity(Parser parser,
                                DFA dfa,
                                int i,
                                int i1,
                                boolean b,
                                BitSet bitSet,
                                ATNConfigSet atnConfigSet) {

    }

    @Override
    public void reportAttemptingFullContext(Parser parser,
                                            DFA dfa,
                                            int i,
                                            int i1,
                                            BitSet bitSet,
                                            ATNConfigSet atnConfigSet) {

    }

    @Override
    public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atnConfigSet) {
    }

    /**
     * Processing include../require.. statements, trying to process include files
     */
    @Override
    public void enterPreprocessorExpression(PHPParser.PreprocessorExpressionContext ctx) {
        String file = extractIncludeName(ctx.expression().getText());
        if (file != null) {
            this.graph.process(file, this.file);
        }
    }

    @Override
    public void enterFunctionDeclaration(PHPParser.FunctionDeclarationContext ctx) {
        Def fnDef = def(ctx.identifier(), DefKind.FUNCTION);
        fnDef.defKey = new DefKey(null, fnDef.name);
        emit(fnDef);

        graph.vars.push(new HashMap<>());
        blockName = fnDef.name;

        // TODO (alexsaveliev): what is ctx.typeParameterListInBrackets()?
        List<PHPParser.FormalParameterContext> fnParams = ctx.formalParameterList().formalParameter();
        if (fnParams == null) {
            return;
        }
        for (PHPParser.FormalParameterContext fnParam : fnParams) {
            PHPParser.TypeHintContext typeHint = fnParam.typeHint();
            if (typeHint != null) {
                ParserRuleContext qName = qName(typeHint.qualifiedStaticTypeRef());
                if (qName != null) {
                    Ref typeRef = ref(qName);
                    // TODO (alexsaveliev) namespace resolution - we should combine name with namespace prefix
                    typeRef.defKey = new DefKey(null, qName.getText());
                    emit(typeRef);
                }
            }
            Def fnArgDef = def(fnParam.variableInitializer().VarName().getSymbol(), DefKind.ARGUMENT);
            fnArgDef.defKey = new DefKey(null, fnDef.defKey.getPath() + '/' + fnArgDef.name);
            emit(fnArgDef);
        }
    }

    @Override
    public void exitFunctionDeclaration(PHPParser.FunctionDeclarationContext ctx) {
        graph.vars.pop();
        blockName = null;
    }

    @Override
    public void enterFunctionCall(PHPParser.FunctionCallContext ctx) {
        PHPParser.FunctionCallNameContext fnCallNameCtx = ctx.functionCallName();
        PHPParser.QualifiedNamespaceNameContext qNameCtx = fnCallNameCtx.qualifiedNamespaceName();
        if (qNameCtx != null) {
            Ref fnRef = ref(qNameCtx);
            fnRef.defKey = new DefKey(null, qNameCtx.getText());
            emit(fnRef);
        }
        // TODO (alexsaveliev): FOO::BAR()
    }

    @Override
    public void enterKeyedVariable(PHPParser.KeyedVariableContext ctx) {
        TerminalNode varNameNode = ctx.VarName();
        if (varNameNode == null) {
            return;
        }
        String varName = varNameNode.getText();
        Map<String, Boolean> localVars = graph.vars.peek();
        Boolean local = localVars.get(varName);
        if (local == null) {
            // new variable
            Def varDef = def(varNameNode.getSymbol(), DefKind.VARIABLE);
            if (blockName != null) {
                varDef.local = true;
                varDef.exported = false;
                local = true;
            } else {
                varDef.local = false;
                varDef.exported = true;
                local = false;
            }
            varDef.defKey = new DefKey(null, blockName != null ? blockName + '/' + varName : varName);
            emit(varDef);
            localVars.put(varName, local);
        } else {
            Ref varRef = ref(varNameNode);
            varRef.defKey = new DefKey(null, local ? blockName + '/' + varName : varName);
            emit(varRef);
        }
    }

    @Override
    public void enterGlobalStatement(PHPParser.GlobalStatementContext ctx) {
        if (this.graph.vars.size() < 2) {
            return;
        }
        List<PHPParser.GlobalVarContext> vars = ctx.globalVar();
        if (vars == null) {
            return;
        }
        for (PHPParser.GlobalVarContext var : vars) {
            TerminalNode varNameNode = var.VarName();
            if (varNameNode != null) {
                String varName = varNameNode.getText();
                if (!this.graph.vars.firstElement().containsKey(varName)) {
                    continue;
                }
                Ref globalVarRef = ref(varNameNode);
                globalVarRef.defKey = new DefKey(null, varName);
                emit(globalVarRef);
                this.graph.vars.peek().put(varName, false);
            }
        }
    }

    @Override
    public void enterClassDeclaration(PHPParser.ClassDeclarationContext ctx) {
        TerminalNode interfaceNode = ctx.Interface();
        blockName = ctx.identifier().getText();
        currentClassInfo = new ClassInfo();
        this.graph.classes.put(blockName, currentClassInfo);

        if (interfaceNode != null) {
            Def interfaceDef = def(ctx.identifier(), DefKind.INTERFACE);
            interfaceDef.defKey = new DefKey(null, interfaceDef.name);
            emit(interfaceDef);

            PHPParser.InterfaceListContext interfaces = ctx.interfaceList();
            if (interfaces == null) {
                return;
            }
            List<PHPParser.QualifiedStaticTypeRefContext> iNames = interfaces.qualifiedStaticTypeRef();
            if (iNames == null) {
                return;
            }
            for (PHPParser.QualifiedStaticTypeRefContext i : iNames) {
                ParserRuleContext qName = qName(i);
                if (qName == null) {
                    continue;
                }
                String extendsInterfaceName = qName.getText();
                Ref extendsInterfaceRef = ref(qName);
                extendsInterfaceRef.defKey = new DefKey(null, extendsInterfaceName);
                emit(extendsInterfaceRef);
                currentClassInfo.extendsClasses.add(extendsInterfaceName);
            }

        } else {
            PHPParser.ClassEntryTypeContext classEntryTypeContext = ctx.classEntryType();
            Def classOrTraitDef = def(ctx.identifier(),
                    classEntryTypeContext.Trait() != null ? DefKind.TRAIT : DefKind.CLASS);
            classOrTraitDef.defKey = new DefKey(null, classOrTraitDef.name);
            emit(classOrTraitDef);

            PHPParser.QualifiedStaticTypeRefContext extendsCtx = ctx.qualifiedStaticTypeRef();
            ParserRuleContext qName = qName(extendsCtx);
            if (qName != null) {
                String extendsName = qName.getText();
                Ref extendsRef = ref(qName);
                extendsRef.defKey = new DefKey(null, extendsName);
                emit(extendsRef);
                currentClassInfo.extendsClasses.add(extendsName);
            }

            PHPParser.InterfaceListContext interfaces = ctx.interfaceList();
            if (interfaces == null) {
                return;
            }

            List<PHPParser.QualifiedStaticTypeRefContext> iNames = interfaces.qualifiedStaticTypeRef();
            if (iNames == null) {
                return;
            }
            for (PHPParser.QualifiedStaticTypeRefContext i : iNames) {
                qName = qName(i);
                if (qName == null) {
                    continue;
                }
                String implementsInterfaceName = qName.getText();
                Ref implementsInterfaceRef = ref(qName);
                implementsInterfaceRef.defKey = new DefKey(null, implementsInterfaceName);
                emit(implementsInterfaceRef);
                currentClassInfo.implementsClasses.add(implementsInterfaceName);
            }
        }
    }

    @Override
    public void exitClassDeclaration(PHPParser.ClassDeclarationContext ctx) {
        blockName = null;
    }

    private static String extractIncludeName(String expressionText) {
        Matcher m = INCLUDE_EXPRESSION.matcher(expressionText);
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }
    private static ParserRuleContext qName(PHPParser.QualifiedStaticTypeRefContext ctx) {
        if (ctx == null) {
            return null;
        }
        return ctx.qualifiedNamespaceName();
    }

}