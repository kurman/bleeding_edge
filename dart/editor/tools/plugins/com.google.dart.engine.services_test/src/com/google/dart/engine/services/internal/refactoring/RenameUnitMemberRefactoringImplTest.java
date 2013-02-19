/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.services.status.RefactoringStatusSeverity;

/**
 * Test for {@link RenameUnitMemberRefactoringImpl}.
 */
public class RenameUnitMemberRefactoringImplTest extends RenameRefactoringImplTest {
  public void test_checkFinalConditions_hasTopLevel_ClassElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "class NewName {} // existing");
    createRenameRefactoring("Test {}");
    // check status
    refactoring.setNewName("NewName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Library already declares class with name 'NewName'.",
        findRangeIdentifier("NewName {} // existing"));
  }

  public void test_checkFinalConditions_hasTopLevel_FunctionTypeAliasElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "typedef NewName(); // existing");
    createRenameRefactoring("Test {}");
    // check status
    refactoring.setNewName("NewName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Library already declares function type alias with name 'NewName'.",
        findRangeIdentifier("NewName(); // existing"));
  }

  public void test_checkFinalConditions_shadowedBy_MethodElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "class A {",
        "  NewName() {}",
        "  main() {",
        "    new Test();",
        "  }",
        "}");
    createRenameRefactoring("Test {}");
    // check status
    refactoring.setNewName("NewName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Reference to renamed class will shadowed by method 'A.NewName'.",
        findRangeIdentifier("NewName() {}"));
  }

  public void test_checkFinalConditions_shadows_MethodElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "class A {",
        "  NewName() {}",
        "}",
        "class B extends A {",
        "  main() {",
        "    NewName(); // super-ref",
        "  }",
        "}");
    createRenameRefactoring("Test {}");
    // check status
    refactoring.setNewName("NewName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Renamed class will shadow method 'A.NewName'.",
        findRangeIdentifier("NewName(); // super-ref"));
  }

  public void test_checkFinalConditionsOK_qualifiedSuper_MethodElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "class A {",
        "  NewName() {}",
        "}",
        "class B extends A {",
        "  main() {",
        "    super.NewName(); // super-ref",
        "  }",
        "}");
    createRenameRefactoring("Test {}");
    // check status
    refactoring.setNewName("NewName");
    assertRefactoringStatusOK(refactoring.checkFinalConditions(pm));
  }

  public void test_checkInitialConditions_ClassElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}");
    createRenameRefactoring("Test {}");
    // null
    refactoring.setNewName(null);
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Class name must not be null.");
    // empty
    refactoring.setNewName("");
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Class name must not be empty.");
    // same name
    refactoring.setNewName("Test");
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.FATAL,
        "Choose another name.");
  }

  public void test_checkInitialConditions_FunctionElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}");
    createRenameRefactoring("test() {");
    // null
    refactoring.setNewName(null);
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Function name must not be null.");
    // empty
    refactoring.setNewName("");
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Function name must not be empty.");
    // same name
    refactoring.setNewName("test");
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.FATAL,
        "Choose another name.");
  }

  public void test_createChange_ClassElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "  Test() {}",
        "  Test.named() {}",
        "}",
        "class Other {",
        "  factory Other.a() = Test;",
        "  factory Other.b() = Test.named;",
        "}",
        "main() {",
        "  Test t1 = new Test();",
        "  Test t2 = new Test.named();",
        "}");
    // configure refactoring
    createRenameRefactoring("Test {");
    assertEquals("Rename Class", refactoring.getRefactoringName());
    refactoring.setNewName("NewName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class NewName {",
        "  NewName() {}",
        "  NewName.named() {}",
        "}",
        "class Other {",
        "  factory Other.a() = NewName;",
        "  factory Other.b() = NewName.named;",
        "}",
        "main() {",
        "  NewName t1 = new NewName();",
        "  NewName t2 = new NewName.named();",
        "}");
  }

  public void test_createChange_ClassElement_typedef() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "typedef Test = Object with A;",
        "main() {",
        "  Test t = new Test();",
        "}");
    // configure refactoring
    createRenameRefactoring("Test =");
    assertEquals("Rename Class", refactoring.getRefactoringName());
    refactoring.setNewName("NewName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "typedef NewName = Object with A;",
        "main() {",
        "  NewName t = new NewName();",
        "}");
  }

  public void test_createChange_FunctionElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "main() {",
        "  print(test);",
        "  print(test());",
        "}");
    // configure refactoring
    createRenameRefactoring("test() {}");
    assertEquals("Rename Top-Level Function", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "newName() {}",
        "main() {",
        "  print(newName);",
        "  print(newName());",
        "}");
  }

  public void test_createChange_PrefixElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math' as test;",
        "main() {",
        "  print(test.PI);",
        "}");
    // configure refactoring
    createRenameRefactoring("test.PI");
    // TODO(scheglov) currently PrefixElement has no enclosing Element, so no refactoring
//    assertEquals("Rename Import Prefix", refactoring.getRefactoringName());
//    refactoring.setNewName("newName");
//    // validate change
//    assertSuccessfulRename(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "import 'dart:math' as newName;",
//        "main() {",
//        "  print(newName.PI);",
//        "}");
  }

  public void test_createChange_TopLevelVariableElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int test = 42;",
        "main() {",
        "  print(test);",
        "}");
    // configure refactoring
    createRenameRefactoring("test = 42;");
    assertEquals("Rename Top-Level Variable", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    // TODO(scheglov) top-level variable is not yet resolved correctly
//    assertSuccessfulRename(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "int newName = 42;",
//        "main() {",
//        "  print(newName);",
//        "}");
  }

  public void test_createChange_TypeAliasElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "main2(Test t) {",
        "}");
    // configure refactoring
    createRenameRefactoring("Test();");
    assertEquals("Rename Function Type Alias", refactoring.getRefactoringName());
    refactoring.setNewName("NewName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef NewName();",
        "main2(NewName t) {",
        "}");
  }
}
