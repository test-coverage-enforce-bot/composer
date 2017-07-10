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

import org.antlr.v4.runtime.Token;
import org.ballerinalang.composer.service.workspace.common.Utils;
import org.ballerinalang.composer.service.workspace.langserver.SymbolInfo;
import org.ballerinalang.composer.service.workspace.langserver.dto.CompletionItem;
import org.ballerinalang.composer.service.workspace.model.ModelPackage;
import org.ballerinalang.composer.service.workspace.suggetions.SuggestionsFilterDataModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AnnotationAttachmentResolver
 */
public class AnnotationAttachmentResolver extends AbstractItemResolver {
    @Override
    public ArrayList<CompletionItem> resolveItems(SuggestionsFilterDataModel dataModel, ArrayList<SymbolInfo> symbols,
                                           HashMap<Class, AbstractItemResolver> resolvers) {
        return filterAnnotations(dataModel, symbols);
    }

    ArrayList<CompletionItem> filterAnnotations(SuggestionsFilterDataModel dataModel,
                                                ArrayList<SymbolInfo> symbols) {

        Token previousToken = null;
        int tokenIndex = dataModel.getTokenIndex();
        boolean foundChannel0 = false;
        for (int i = (tokenIndex - 1); i > 0; i--) {
            Token token = dataModel.getTokenStream().get(i);
            if (token.getChannel() == 0) {
                foundChannel0 = true;
                if ("@".equals(token.getText())) {
                    previousToken = token;
                    break;
                } else {
                    continue;
                }

            } else {
                if (foundChannel0) {
                    break;
                } else {
                    continue;
                }
            }
        }

        if (previousToken != null && "@".equals(previousToken.getText())) {
            Set<Map.Entry<String, ModelPackage>> packages = dataModel.getPackages();
            if (packages == null) {
                packages = Utils.getAllPackages();
            }
            List<CompletionItem> collect = packages.stream()
                    .map(i -> i.getValue().getAnnotations())
                    .flatMap(Collection::stream).map(i -> {
                        CompletionItem importItem = new CompletionItem();

                        String insertText = lastPart(i.getPackagePath()) + ":" + i.getName();
                        importItem.setLabel("@" + insertText + " (" + i.getPackagePath() + ")");
                        importItem.setInsertText(insertText);
                        importItem.setDetail(ItemResolverConstants.ANNOTATION_TYPE);
                        importItem.setSortText(ItemResolverConstants.PRIORITY_4);
                        return importItem;
                    }).collect(Collectors.toList());

            ArrayList<CompletionItem> list = new ArrayList<>();
            list.addAll(collect);
            return list;

        }
        return null;
    }

    private String lastPart(String packagePath) {
        int i = packagePath.lastIndexOf('.');
        if (i >= 0) {
            return packagePath.substring(i + 1);
        } else {
            return packagePath;
        }
    }
}