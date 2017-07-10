/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/

package org.ballerinalang.composer.service.workspace.langserver.util.resolvers;

import org.antlr.v4.runtime.TokenStream;
import org.ballerinalang.composer.service.workspace.langserver.SymbolInfo;
import org.ballerinalang.composer.service.workspace.langserver.dto.CompletionItem;
import org.ballerinalang.composer.service.workspace.suggetions.SuggestionsFilterDataModel;
import org.ballerinalang.model.BallerinaFunction;
import org.ballerinalang.model.NativeUnit;
import org.ballerinalang.model.ParameterDef;
import org.ballerinalang.model.StructDef;
import org.ballerinalang.model.VariableDef;
import org.ballerinalang.model.types.SimpleTypeName;
import org.ballerinalang.natives.NativePackageProxy;
import org.ballerinalang.natives.NativeUnitProxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Interface for completion item resolvers
 */
public abstract class AbstractItemResolver {
    public abstract ArrayList<CompletionItem> resolveItems(SuggestionsFilterDataModel dataModel,
                                                           ArrayList<SymbolInfo> symbols, HashMap<Class,
            AbstractItemResolver> resolvers);

    public void populateCompletionItemList(List<SymbolInfo> symbolInfoList, List<CompletionItem> completionItems) {

        symbolInfoList.forEach(symbolInfo -> {
            CompletionItem completionItem = new CompletionItem();
            completionItem.setLabel(symbolInfo.getSymbolName());
            String[] delimiterSeparatedTokens = (symbolInfo.getSymbolName()).split("\\.");
            completionItem.setInsertText(delimiterSeparatedTokens[delimiterSeparatedTokens.length - 1]);
            if (symbolInfo.getSymbol() instanceof NativeUnitProxy
                    && symbolInfo.getSymbolName().contains("ClientConnector")) {
                this.populateClientConnectorCompletionItem(completionItem, symbolInfo);
            } else if (symbolInfo.getSymbol() instanceof NativeUnitProxy) {
                this.populateNativeUnitProxyCompletionItem(completionItem, symbolInfo);
            } else if (symbolInfo.getSymbol() instanceof BallerinaFunction) {
                this.populateBallerinaFunctionCompletionItem(completionItem, symbolInfo);
            } else if (symbolInfo.getSymbol() instanceof NativePackageProxy) {
                this.populateNativePackageProxyCompletionItem(completionItem, symbolInfo);
            } else if (symbolInfo.getSymbol() instanceof VariableDef) {
                this.populateVariableDefCompletionItem(completionItem, symbolInfo);
            } else if ((symbolInfo.getSymbol() instanceof StructDef)) {
                this.populateStructDefCompletionItem(completionItem, symbolInfo);
            }
            completionItems.add(completionItem);
        });
    }

    void populateClientConnectorCompletionItem(CompletionItem completionItem, SymbolInfo symbolInfo) {
        completionItem.setDetail(ItemResolverConstants.ACTION_TYPE);
        completionItem.setSortText(ItemResolverConstants.PRIORITY_2);
    }

    void populateNativeUnitProxyCompletionItem(CompletionItem completionItem, SymbolInfo symbolInfo) {
        NativeUnit nativeUnit = ((NativeUnitProxy) symbolInfo.getSymbol()).load();
        completionItem.setLabel(getSignature(symbolInfo, nativeUnit));
        completionItem.setDetail(ItemResolverConstants.FUNCTION_TYPE);
        completionItem.setSortText(ItemResolverConstants.PRIORITY_5);
    }

    void populateBallerinaFunctionCompletionItem(CompletionItem completionItem, SymbolInfo symbolInfo) {
        completionItem.setLabel(getFunctionSignature((BallerinaFunction) symbolInfo.getSymbol()));
        completionItem.setDetail(ItemResolverConstants.FUNCTION_TYPE);
        completionItem.setSortText(ItemResolverConstants.PRIORITY_6);
    }

    void populateNativePackageProxyCompletionItem(CompletionItem completionItem, SymbolInfo symbolInfo) {
        completionItem.setDetail(ItemResolverConstants.PACKAGE_TYPE);
        completionItem.setSortText(ItemResolverConstants.PRIORITY_4);
    }

    void populateVariableDefCompletionItem(CompletionItem completionItem, SymbolInfo symbolInfo) {
        String typeName = ((VariableDef) symbolInfo.getSymbol()).getTypeName().getName();
        completionItem.setDetail((typeName.equals("")) ? ItemResolverConstants.NONE : typeName);
        completionItem.setSortText(ItemResolverConstants.PRIORITY_7);
    }

    void populateStructDefCompletionItem(CompletionItem completionItem, SymbolInfo symbolInfo) {
        completionItem.setLabel(((StructDef) symbolInfo.getSymbol()).getName());
        completionItem.setDetail(ItemResolverConstants.STRUCT_TYPE);
        completionItem.setSortText(ItemResolverConstants.PRIORITY_6);
    }


    private String getSignature(SymbolInfo symbolInfo, NativeUnit nativeUnit) {
        StringBuffer signature = new StringBuffer(symbolInfo.getSymbolName());
        int i = 0;
        String initString = "";
        for (SimpleTypeName simpleTypeName : nativeUnit.getArgumentTypeNames()) {
            signature.append(initString).append(simpleTypeName.getName()).append(" ").
                    append(nativeUnit.getArgumentNames()[i]);
            ++i;
            initString = ", ";
        }
        signature.append(")");
        initString = "(";
        String endString = "";
        for (SimpleTypeName simpleTypeName : nativeUnit.getReturnParamTypeNames()) {
            signature.append(initString).append(simpleTypeName.getName());
            initString = ", ";
            endString = ")";
        }
        signature.append(endString);
        return signature.toString();
    }

    private String getFunctionSignature(BallerinaFunction ballerinaFunction) {
        StringBuffer signature = new StringBuffer(ballerinaFunction.getName());
        String initString = "(";
        for (ParameterDef simpleTypeName : ballerinaFunction.getParameterDefs()) {
            signature.append(initString).append(simpleTypeName.getTypeName()).append(" ").
                    append(simpleTypeName.getName());
            initString = ", ";
        }
        signature.append(")");
        initString = "(";
        String endString = "";
        for (ParameterDef simpleTypeName : ballerinaFunction.getReturnParameters()) {
            signature.append(initString).append(simpleTypeName.getTypeName());
            initString = ", ";
            endString = ")";
        }
        signature.append(endString);
        return signature.toString();
    }

    public boolean isActionOrFunctionInvocationStatement(SuggestionsFilterDataModel dataModel) {
        TokenStream tokenStream = dataModel.getTokenStream();
        int currentTokenIndex = dataModel.getTokenIndex();
        boolean continueSearch = true;
        int searchIndex = currentTokenIndex + 1;

        while (continueSearch) {
            if (tokenStream != null && searchIndex < tokenStream.size()) {
                String tokenStr = tokenStream.get(searchIndex).getText();
                if (tokenStr.equals(":") || tokenStr.equals(".")) {
                    return true;
                }
                searchIndex++;
            } else {
                continueSearch = false;
            }
        }

        return false;
    }
}