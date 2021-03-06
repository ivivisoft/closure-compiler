/*
 * Copyright 2015 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.javascript.jscomp;

import com.google.common.collect.Table;
import com.google.javascript.jscomp.NodeTraversal.AbstractPostOrderCallback;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.Node;

/**
 * Renames references in code and JSDoc when necessary.
 *
 * @author moz@google.com (Michael Zhou)
 */
final class Es6RenameReferences extends AbstractPostOrderCallback {

  private final Table<Node, String, String> renameTable;

  Es6RenameReferences(Table<Node, String, String> renameTable) {
    this.renameTable = renameTable;
  }

  @Override
  public void visit(NodeTraversal t, Node n, Node parent) {
    if (NodeUtil.isReferenceName(n)) {
      renameReference(t, n);
    }

    JSDocInfo info = n.getJSDocInfo();
    if (info != null) {
      renameTypeNode(t, info.getTypeNodes());
    }
  }

  private void renameTypeNode(NodeTraversal t, Iterable<Node> typeNodes) {
    for (Node type : typeNodes) {
      if (type.isString()) {
        renameReference(t, type);
      }
      renameTypeNode(t, type.children());
    }
  }

  private void renameReference(NodeTraversal t, Node n) {
    String oldName = n.getString();
    Scope current = t.getScope();
    while (current != null) {
      String newName = renameTable.get(current.getRootNode(), oldName);
      if (newName != null) {
        n.setString(newName);
        t.reportCodeChange();
        return;
      } else if (current.isDeclaredSloppy(oldName, false)) {
        return;
      } else {
        current = current.getParent();
      }
    }
  }
}
