/*
 * Copyright 2001-2019 Artima, Inc.
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
package org.scalatest.funspec

import org.scalatest._
import org.scalatest.exceptions._
import org.scalactic.{source, Prettifier}
import org.scalatest.verbs.BehaveWord
import Suite.autoTagClassAnnotations
import org.scalatest.PathEngine.isInTargetPath

/**
 * Implementation trait for class <code>Path.FunSpec</code>, which is
 * a sister class to <code>org.scalatest.funspec.AnyFunSpec</code> that isolates
 * tests by running each test in its own instance of the test class,
 * and for each test, only executing the <em>path</em> leading to that test.
 * 
 * <p>
 * <a href="PathAnyFunSpec.html"><code>PathAnyFunSpec</code></a> is a class, not a trait,
 * to minimize compile time given there is a slight compiler overhead to
 * mixing in traits compared to extending classes. If you need to mix the
 * behavior of <code>PathAnyFunSpec</code> into some other class, you can use this
 * trait instead, because class <code>PathAnyFunSpec</code> does nothing more than
 * extend this trait and add a nice <code>toString</code> implementation.
 * </p>
 *
 * <p>
 * See the documentation of the class for a <a href="PathAnyFunSpec.html">detailed
 * overview of <code>PathAnyFunSpec</code></a>.
 * </p>
 *
 * @author Bill Venners
 */
@Finders(Array("org.scalatest.finders.FunSpecFinder"))
//SCALATESTJS-ONLY @scala.scalajs.reflect.annotation.EnableReflectiveInstantiation
//SCALATESTNATIVE-ONLY @scala.scalanative.reflect.annotation.EnableReflectiveInstantiation
trait PathAnyFunSpecLike extends org.scalatest.Suite with OneInstancePerTest with Informing with Notifying with Alerting with Documenting { thisSuite =>
  
  private final val engine = PathEngine.getEngine()
  import engine._

  // SKIP-SCALATESTJS,NATIVE-START
  override def newInstance: org.scalatest.funspec.PathAnyFunSpecLike = this.getClass.newInstance.asInstanceOf[PathAnyFunSpecLike]
  // SKIP-SCALATESTJS,NATIVE-END
  //SCALATESTJS,NATIVE-ONLY override def newInstance: org.scalatest.funspec.PathAnyFunSpecLike

  /**
   * Returns an <code>Informer</code> that during test execution will forward strings (and other objects) passed to its
   * <code>apply</code> method to the current reporter. If invoked in a constructor (including within a test, since
   * those are invoked during construction in a <code>PathAnyFunSpec</code>, it
   * will register the passed string for forwarding later when <code>run</code> is invoked. If invoked from inside a test function,
   * it will record the information and forward it to the current reporter only after the test completed, as <code>recordedEvents</code>
   * of the test completed event, such as <code>TestSucceeded</code>.  If invoked at any other time, it will print to the standard output.
   * This method can be called safely by any thread.
   */
  protected def info: Informer = atomicInformer.get

  /**
   * Returns a <code>Notifier</code> that during test execution will forward strings passed to its
   * <code>apply</code> method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * <code>PathAnyFunSpec</code> is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * print to the standard output. This method can be called safely by any thread.
   */
  protected def note: Notifier = atomicNotifier.get

  /**
   * Returns an <code>Alerter</code> that during test execution will forward strings passed to its
   * <code>apply</code> method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * <code>PathAnyFunSpec</code> is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * print to the standard output. This method can be called safely by any thread.
   */
  protected def alert: Alerter = atomicAlerter.get

  /**
   * Returns a <code>Documenter</code> that during test execution will forward strings (and other objects) passed to its
   * <code>apply</code> method to the current reporter. If invoked in a constructor (including within a test, since
   * those are invoked during construction in a <code>PathAnyFunSpec</code>, it
   * will register the passed string for forwarding later when <code>run</code> is invoked. If invoked from inside a test function,
   * it will record the information and forward it to the current reporter only after the test completed, as <code>recordedEvents</code>
   * of the test completed event, such as <code>TestSucceeded</code>.  If invoked at any other time, it will print to the standard output.
   * This method can be called safely by any thread.
   */
  protected def markup: Documenter = atomicDocumenter.get

  /**
   * Class that, via an instance referenced from the <code>it</code> field,
   * supports test (and shared test) registration in <code>PathAnyFunSpec</code>s.
   *
   * <p>
   * This class supports syntax such as the following test registration:
   * </p>
   *
   * <pre class="stExamples">
   * it("should be empty")
   * ^
   * </pre>
   *
   * <p>
   * and the following shared test registration:
   * </p>
   *
   * <pre class="stExamples">
   * it should behave like nonFullStack(stackWithOneItem)
   * ^
   * </pre>
   *
   * <p>
   * For more information and examples, see the <a href="PathAnyFunSpec.html">main documentation for <code>PathAnyFunSpec</code></a>.
   * </p>
   */
  protected class ItWord {

    private final def applyImpl(testText: String, testTags: Tag*)(testFun: => Unit /* Assertion */, pos: source.Position): Unit = {
      // SKIP-SCALATESTJS,NATIVE-START
      val stackDepth = 3
      val stackDepthAdjustment = -2
      // SKIP-SCALATESTJS,NATIVE-END
      //SCALATESTJS,NATIVE-ONLY val stackDepth = 5
      //SCALATESTJS,NATIVE-ONLY val stackDepthAdjustment = -4
      handleTest(thisSuite, testText, Transformer(() => testFun), Resources.itCannotAppearInsideAnotherItOrThey, "PathAnyFunSpecLike.scala", "apply", stackDepth, stackDepthAdjustment, None, Some(pos), testTags: _*)
    }

    /**
     * Supports test registration.
     *
     * <p>
     * This trait's implementation of this method will decide whether to register the text and invoke the passed function
     * based on whether or not this is part of the current "test path." For the details on this process, see
     * the <a href="#howItExecutes">How it executes</a> section of the main documentation for
     * trait <code>org.scalatest.funspec.PathAnyFunSpec</code>.
     * </p>
     *
     * @param testText the test text, which will be combined with the descText of any surrounding describers
     * to form the test name
     * @param testTags the optional list of tags for this test
     * @param testFun the test function
     * @throws DuplicateTestNameException if a test with the same name has been registered previously
     * @throws TestRegistrationClosedException if invoked after <code>run</code> has been invoked on this suite
     * @throws NullArgumentException if <code>specText</code> or any passed test tag is <code>null</code>
     */
    // SKIP-DOTTY-START
    def apply(testText: String, testTags: Tag*)(testFun: => Unit /* Assertion */)(implicit pos: source.Position): Unit = {
      applyImpl(testText, testTags: _*)(testFun, pos)
    }
    // SKIP-DOTTY-END
    //DOTTY-ONLY inline def apply(testText: String, testTags: Tag*)(testFun: => Unit /* Assertion */): Unit = {
    //DOTTY-ONLY   ${ source.Position.withPosition[Unit]('{(pos: source.Position) => applyImpl(testText, testTags: _*)(testFun, pos) }) } 
    //DOTTY-ONLY }
    
    /**
     * Supports the registration of shared tests.
     *
     * <p>
     * This method supports syntax such as the following:
     * </p>
     *
     * <pre class="stExamples">
     * it should behave like nonFullStack(stackWithOneItem)
     *    ^
     * </pre>
     *
     * <p>
     * For examples of shared tests, see the <a href="PathAnyFunSpec.html#SharedTests">Shared tests section</a>
     * in the main documentation for trait <code>org.scalatest.funspec.PathAnyFunSpec</code>.
     * </p>
     */
    def should(behaveWord: BehaveWord) = behaveWord

    /**
     * Supports the registration of shared tests.
     *
     * <p>
     * This method supports syntax such as the following:
     * </p>
     *
     * <pre class="stExamples">
     * it must behave like nonFullStack(stackWithOneItem)
     *    ^
     * </pre>
     *
     * <p>
     * For examples of shared tests, see the <a href="PathAnyFunSpec.html#SharedTests">Shared tests section</a>
     * in the main documentation for trait <code>org.scalatest.funspec.PathAnyFunSpec</code>.
     * </p>
     */
    def must(behaveWord: BehaveWord) = behaveWord
  }

  /**
   * Supports test (and shared test) registration in <code>PathAnyFunSpec</code>s.
   *
   * <p>
   * This field supports syntax such as the following:
   * </p>
   *
   * <pre class="stExamples">
   * it("should be empty")
   * ^
   * </pre>
   *
   * <pre> class="stExamples"
   * it should behave like nonFullStack(stackWithOneItem)
   * ^
   * </pre>
   *
   * <p>
   * For more information and examples of the use of the <code>it</code> field, see the main documentation for this trait.
   * </p>
   */
  protected val it = new ItWord

  /**
   * Class that, via an instance referenced from the <code>they</code> field,
   * supports test (and shared test) registration in <code>PathAnyFunSpec</code>s.
   *
   * <p>
   * This class supports syntax such as the following test registration:
   * </p>
   *
   * <pre class="stExamples">
   * they("should be empty")
   * ^
   * </pre>
   *
   * <p>
   * and the following shared test registration:
   * </p>
   *
   * <pre class="stExamples">
   * they should behave like nonFullStack(stackWithOneItem)
   * ^
   * </pre>
   *
   * <p>
   * For more information and examples, see the <a href="PathAnyFunSpec.html">main documentation for <code>PathAnyFunSpec</code></a>.
   * </p>
   */
  protected class TheyWord {

    private def applyImpl(testText: String, testTags: Tag*)(testFun: => Unit /* Assertion */, pos: source.Position): Unit = {
      handleTest(thisSuite, testText, Transformer(() => testFun), Resources.theyCannotAppearInsideAnotherItOrThey, "PathAnyFunSpecLike.scala", "apply", 3, -2, None, Some(pos), testTags: _*)
    }

    /**
     * Supports test registration.
     *
     * <p>
     * This trait's implementation of this method will decide whether to register the text and invoke the passed function
     * based on whether or not this is part of the current "test path." For the details on this process, see
     * the <a href="#howItExecutes">How it executes</a> section of the main documentation for
     * trait <code>org.scalatest.funspec.PathAnyFunSpec</code>.
     * </p>
     *
     * @param testText the test text, which will be combined with the descText of any surrounding describers
     * to form the test name
     * @param testTags the optional list of tags for this test
     * @param testFun the test function
     * @throws DuplicateTestNameException if a test with the same name has been registered previously
     * @throws TestRegistrationClosedException if invoked after <code>run</code> has been invoked on this suite
     * @throws NullArgumentException if <code>specText</code> or any passed test tag is <code>null</code>
     */
    // SKIP-DOTTY-START
    def apply(testText: String, testTags: Tag*)(testFun: => Unit /* Assertion */)(implicit pos: source.Position): Unit = {
      applyImpl(testText, testTags: _*)(testFun, pos)
    }
    // SKIP-DOTTY-END
    //DOTTY-ONLY inline def apply(testText: String, testTags: Tag*)(testFun: => Unit /* Assertion */): Unit = {
    //DOTTY-ONLY   ${ source.Position.withPosition[Unit]('{(pos: source.Position) => applyImpl(testText, testTags: _*)(testFun, pos) }) } 
    //DOTTY-ONLY }
 
    /**
     * Supports the registration of shared tests.
     *
     * <p>
     * This method supports syntax such as the following:
     * </p>
     *
     * <pre class="stExamples">
     * they should behave like nonFullStack(stackWithOneItem)
     *      ^
     * </pre>
     *
     * <p>
     * For examples of shared tests, see the <a href="PathAnyFunSpec.html#SharedTests">Shared tests section</a>
     * in the main documentation for trait <code>org.scalatest.funspec.PathAnyFunSpec</code>.
     * </p>
     */
    def should(behaveWord: BehaveWord) = behaveWord

    /**
     * Supports the registration of shared tests.
     *
     * <p>
     * This method supports syntax such as the following:
     * </p>
     *
     * <pre class="stExamples">
     * they must behave like nonFullStack(stackWithOneItem)
     *      ^
     * </pre>
     *
     * <p>
     * For examples of shared tests, see the <a href="PathAnyFunSpec.html#SharedTests">Shared tests section</a>
     * in the main documentation for trait <code>org.scalatest.funspec.PathAnyFunSpec</code>.
     * </p>
     */
    def must(behaveWord: BehaveWord) = behaveWord
  }

  /**
   * Supports test (and shared test) registration in <code>PathAnyFunSpec</code>s.
   *
   * <p>
   * This field supports syntax such as the following:
   * </p>
   *
   * <pre class="stExamples">
   * it("should be empty")
   * ^
   * </pre>
   *
   * <pre> class="stExamples"
   * it should behave like nonFullStack(stackWithOneItem)
   * ^
   * </pre>
   *
   * <p>
   * For more information and examples of the use of the <code>it</code> field, see the main documentation for this trait.
   * </p>
   */
  protected val they = new TheyWord

  private final def ignoreImpl(testText: String, testTags: Tag*)(testFun: => Unit /* Assertion */, pos: source.Position): Unit = {
    // SKIP-SCALATESTJS,NATIVE-START
    val stackDepth = 4
    val stackDepthAdjustment = -2
    // SKIP-SCALATESTJS,NATIVE-END
    //SCALATESTJS,NATIVE-ONLY val stackDepth = 6
    //SCALATESTJS,NATIVE-ONLY val stackDepthAdjustment = -4
    // Might not actually register it. Only will register it if it is its turn.
    handleIgnoredTest(testText, Transformer(() => testFun), Resources.ignoreCannotAppearInsideAnItOrAThey, "PathAnyFunSpecLike.scala", "ignore", stackDepth, stackDepthAdjustment, None, Some(pos), testTags: _*)
  }
  
  /**
   * Supports registration of a test to ignore.
   *
   * <p>
   * For more information and examples of this method's use, see the
   * <a href="AnyFunSpec.html#ignoredTests">Ignored tests</a> section in the main documentation for sister
   * trait <code>org.scalatest.funspec.AnyFunSpec</code>. Note that a separate instance will be created for an ignored test,
   * and the path to the ignored test will be executed in that instance, but the test function itself will not
   * be executed. Instead, a <code>TestIgnored</code> event will be fired.
   * </p>
   *
   * @param testText the specification text, which will be combined with the descText of any surrounding describers
   * to form the test name
   * @param testTags the optional list of tags for this test
   * @param testFun the test function
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws TestRegistrationClosedException if invoked after <code>run</code> has been invoked on this suite
   * @throws NullArgumentException if <code>specText</code> or any passed test tag is <code>null</code>
   */
  // SKIP-DOTTY-START
  protected def ignore(testText: String, testTags: Tag*)(testFun: => Unit /* Assertion */)(implicit pos: source.Position): Unit = {
    ignoreImpl(testText, testTags: _*)(testFun, pos)
  }
  // SKIP-DOTTY-END
  //DOTTY-ONLY inline def ignore(testText: String, testTags: Tag*)(testFun: => Unit /* Assertion */): Unit = {
  //DOTTY-ONLY   ${ source.Position.withPosition[Unit]('{(pos: source.Position) => ignoreImpl(testText, testTags: _*)(testFun, pos) }) } 
  //DOTTY-ONLY }
  
  private final def describeImpl(description: String)(fun: => Unit, pos: source.Position): Unit = {
    // SKIP-SCALATESTJS,NATIVE-START
    val stackDepth = 4
    // SKIP-SCALATESTJS,NATIVE-END
    //SCALATESTJS,NATIVE-ONLY val stackDepth = 6

    try {
      handleNestedBranch(description, None, fun, Resources.describeCannotAppearInsideAnIt, "PathAnyFunSpecLike.scala", "describe", stackDepth, -2, None, Some(pos))
    }
    catch {
      case e: TestFailedException => throw new NotAllowedException(FailureMessages.assertionShouldBePutInsideItOrTheyClauseNotDescribeClause, Some(e), e.position.getOrElse(pos))
      case e: TestCanceledException => throw new NotAllowedException(FailureMessages.assertionShouldBePutInsideItOrTheyClauseNotDescribeClause, Some(e), e.position.getOrElse(pos))
      case e: DuplicateTestNameException => throw new NotAllowedException(FailureMessages.exceptionWasThrownInDescribeClause(Prettifier.default, UnquotedString(e.getClass.getName), description, e.getMessage), Some(e), e.position.getOrElse(pos))
      case other: Throwable if (!Suite.anExceptionThatShouldCauseAnAbort(other)) => throw new NotAllowedException(FailureMessages.exceptionWasThrownInDescribeClause(Prettifier.default, UnquotedString(other.getClass.getName), description, other.getMessage), Some(other), pos)
      case other: Throwable => throw other
    }
  }

  /**
   * Describe a &ldquo;subject&rdquo; being specified and tested by the passed function value. The
   * passed function value may contain more describers (defined with <code>describe</code>) and/or tests
   * (defined with <code>it</code>).
   *
   * <p>
   * This class's implementation of this method will decide whether to
   * register the description text and invoke the passed function
   * based on whether or not this is part of the current "test path." For the details on this process, see
   * the <a href="#howItExecutes">How it executes</a> section of the main documentation for trait
   * <code>org.scalatest.funspec.PathAnyFunSpec</code>.
   * </p>
   */
  // SKIP-DOTTY-START
  protected def describe(description: String)(fun: => Unit)(implicit pos: source.Position): Unit = {
    describeImpl(description)(fun, pos)
  }
  // SKIP-DOTTY-END
  //DOTTY-ONLY inline def describe(description: String)(fun: => Unit): Unit = {
  //DOTTY-ONLY   ${ source.Position.withPosition[Unit]('{(pos: source.Position) => describeImpl(description)(fun, pos) }) }
  //DOTTY-ONLY }
  
  /**
   * Supports shared test registration in <code>PathAnyFunSpec</code>s.
   *
   * <p>
   * This field supports syntax such as the following:
   * </p>
   *
   * <pre class="stExamples">
   * it should behave like nonFullStack(stackWithOneItem)
   *           ^
   * </pre>
   *
   * <p>
   * For more information and examples of the use of <cod>behave</code>, see the
   * <a href="AnyFunSpec.html#SharedTests">Shared tests</a> section in the main documentation for sister
   * trait <code>org.scalatest.funspec.AnyFunSpec</code>.
   * </p>
   */
  protected val behave = new BehaveWord

  /**
   * An immutable <code>Set</code> of test names. If this <code>PathAnyFunSpec</code> contains no tests, this method returns an
   * empty <code>Set</code>.
   *
   * <p>
   * This trait's implementation of this method will first ensure that the results of all tests, each run its its
   * own instance executing only the path to the test, are registered. For details on this process see the
   * <a href="#howItExecutes">How it executes</a> section in the main documentation for this trait.
   * </p>
   *
   * <p>
   * This trait's implementation of this method will return a set that contains the names of all registered tests. The set's
   * iterator will return those names in the order in which the tests were registered. Each test's name is composed
   * of the concatenation of the text of each surrounding describer, in order from outside in, and the text of the
   * example itself, with all components separated by a space. For example, consider this <code>PathAnyFunSpec</code>:
   * </p>
   *
   * <pre class="stHighlight">
   * import org.scalatest.funspec
   *
   * class StackSpec extends funspec.PathAnyFunSpec {
   *   describe("A Stack") {
   *     describe("when not empty") {
   *       "must allow me to pop" in {}
   *     }
   *     describe("when not full") {
   *       "must allow me to push" in {}
   *     }
   *   }
   * }
   * </pre>
   *
   * <p>
   * Invoking <code>testNames</code> on this <code>PathAnyFunSpec</code> will yield a set that contains the following
   * two test name strings:
   * </p>
   *
   * <pre>
   * "A Stack when not empty must allow me to pop"
   * "A Stack when not full must allow me to push"
   * </pre>
   *
   * <p>
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * </p>
   */
  final override def testNames: Set[String] = {
    ensureTestResultsRegistered(thisSuite)
    InsertionOrderSet(atomic.get.testNamesList)
  }

  /**
   * The total number of tests that are expected to run when this <code>PathAnyFunSpec</code>'s <code>run</code> method
   * is invoked.
   *
   * <p>
   * This trait's implementation of this method will first ensure that the results of all tests, each run its its
   * own instance executing only the path to the test, are registered. For details on this process see the
   * <a href="#howItExecutes">How it executes</a> section in the main documentation for this trait.
   * </p>
   *
   * <p>
   * This trait's implementation of this method returns the size of the <code>testNames</code> <code>List</code>, minus
   * the number of tests marked as ignored as well as any tests excluded by the passed <code>Filter</code>.
   * </p>
   *
   * <p>
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * </p>
   *
   * @param filter a <code>Filter</code> with which to filter tests to count based on their tags
   */
  final override def expectedTestCount(filter: Filter): Int = {
    ensureTestResultsRegistered(thisSuite)
    super.expectedTestCount(filter)
  }

  /**
   * Runs a test.
   *
   * <p>
   * This trait's implementation of this method will first ensure that the results of all tests, each run its its
   * own instance executing only the path to the test, are registered. For details on this process see the
   * <a href="#howItExecutes">How it executes</a> section in the main documentation for this trait.
   * </p>
   *
   * <p>
   * This trait's implementation reports the test results registered with the name specified by
   * <code>testName</code>. Each test's name is a concatenation of the text of all describers surrounding a test,
   * from outside in, and the test's  spec text, with one space placed between each item. (See the documentation
   * for <code>testNames</code> for an example.)
   *
   * <p>
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * </p>
   *
   * @param testName the name of one test to execute.
   * @param reporter the <code>Reporter</code> to which results will be reported
   * @param stopper the <code>Stopper</code> that will be consulted to determine whether to stop execution early.
   * @param configMap a <code>Map</code> of properties that can be used by this <code>FreeSpec</code>'s executing tests.
   * @throws NullArgumentException if any of <code>testName</code>, <code>reporter</code>, <code>stopper</code>, or <code>configMap</code>
   *     is <code>null</code>.
   */
  final protected override def runTest(testName: String, args: Args): Status = {

    ensureTestResultsRegistered(thisSuite)
    
    def dontInvokeWithFixture(theTest: TestLeaf): Outcome = {
      theTest.testFun()
    }

    runTestImpl(thisSuite, testName, args, true, dontInvokeWithFixture)
  }

  /**
   * A <code>Map</code> whose keys are <code>String</code> tag names to which tests in this <code>path.FreeSpec</code>
   * belong, and values the <code>Set</code> of test names that belong to each tag. If this <code>path.FreeSpec</code>
   * contains no tags, this method returns an empty <code>Map</code>.
   *
   * <p>
   * This trait's implementation of this method will first ensure that the results of all tests, each run its its
   * own instance executing only the path to the test, are registered. For details on this process see the
   * <a href="#howItExecutes">How it executes</a> section in the main documentation for this trait.
   * </p>
   *
   * <p>
   * This trait's implementation returns tags that were passed as strings contained in <code>Tag</code> objects passed
   * to methods <code>it</code> and <code>ignore</code>.
   * </p>
   * 
   * <p>
   * In addition, this trait's implementation will also auto-tag tests with class level annotations.  
   * For example, if you annotate @Ignore at the class level, all test methods in the class will be auto-annotated with @Ignore.
   * </p>
   *
   * <p>
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * </p>
   */
  final override def tags: Map[String, Set[String]] = {
    ensureTestResultsRegistered(thisSuite)
    autoTagClassAnnotations(atomic.get.tagsMap, this)
  }

  /**
   * Runs this <code>PathAnyFunSpec</code>, reporting test results that were registered when the tests
   * were run, each during the construction of its own instance.
   *
   * <p>
   * This trait's implementation of this method will first ensure that the results of all tests, each run its its
   * own instance executing only the path to the test, are registered. For details on this process see the
   * <a href="#howItExecutes">How it executes</a> section in the main documentation for this trait.
   * </p>
   *
   * <p>If <code>testName</code> is <code>None</code>, this trait's implementation of this method
   * will report the registered results for all tests except any excluded by the passed <code>Filter</code>.
   * If <code>testName</code> is defined, it will report the results of only that named test. Because a
   * <code>PathAnyFunSpec</code> is not allowed to contain nested suites, this trait's implementation of
   * this method does not call <code>runNestedSuites</code>.
   * </p>
   *
   * <p>
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * </p>
   *
   * @param testName an optional name of one test to run. If <code>None</code>, all relevant tests should be run.
   *                 I.e., <code>None</code> acts like a wildcard that means run all relevant tests in this <code>Suite</code>.
   * @param args the <code>Args</code> for this run
   *
   *@throws NullArgumentException if any passed parameter is <code>null</code>.
   * @throws IllegalArgumentException if <code>testName</code> is defined, but no test with the specified test name
   *     exists in this <code>Suite</code>
   */
  final override def run(testName: Option[String], args: Args): Status = {
    ensureTestResultsRegistered(thisSuite)
    runPathTestsImpl(thisSuite, testName, args, info, true, runTest)
  }

  /**
   * This lifecycle method is unused by this trait, and will complete abruptly with
   * <code>UnsupportedOperationException</code> if invoked.
   *
   * <p>
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * </p>
   */
  final protected override def runTests(testName: Option[String], args: Args): Status = {
    throw new UnsupportedOperationException
    // ensureTestResultsRegistered(isAnInitialInstance, this)
    // runTestsImpl(thisSuite, testName, reporter, stopper, filter, configMap, distributor, tracker, info, true, runTest)
  }

  /**
   * This lifecycle method is unused by this trait, and is implemented to do nothing. If invoked, it will
   * just return immediately.
   *
   * <p>
   * Nested suites are not allowed in a <code>PathAnyFunSpec</code>. Because
   * a <code>PathAnyFunSpec</code> executes tests eagerly at construction time, registering the results of
   * those test runs and reporting them later, the order of nested suites versus test runs would be different
   * in a <code>org.scalatest.funspec.PathAnyFunSpec</code> than in an <code>org.scalatest.funspec.PathAnyFunSpec</code>. In an
   * <code>org.scalatest.funspec.AnyFunSpec</code>, nested suites are executed then tests are executed. In an
   * <code>org.scalatest.funspec.PathAnyFunSpec</code> it would be the opposite. To make the code easy to reason about,
   * therefore, this is just not allowed. If you want to add nested suites to a <code>PathAnyFunSpec</code>, you can
   * instead wrap them all in a <a href="../Suites.html"><code>Suites</code></a>
   * object and put them in whatever order you wish.
   * </p>
   *
   * <p>
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * </p>
   */
  final protected override def runNestedSuites(args: Args): Status = SucceededStatus

  /**
   * Returns an empty list.
   *
   * <p>
   * This lifecycle method is unused by this trait. If invoked, it will return an empty list, because
   * nested suites are not allowed in a <code>PathAnyFunSpec</code>. Because
   * a <code>PathAnyFunSpec</code> executes tests eagerly at construction time, registering the results of
   * those test runs and reporting them later, the order of nested suites versus test runs would be different
   * in a <code>org.scalatest.funspec.PathAnyFunSpec</code> than in an <code>org.scalatest.funspec.AnyFunSpec</code>. In an
   * <code>org.scalatest.funspec.PathAnyFunSpec</code>, nested suites are executed then tests are executed. In an
   * <code>org.scalatest.funspec.PathAnyFunSpec</code> it would be the opposite. To make the code easy to reason about,
   * therefore, this is just not allowed. If you want to add nested suites to a <code>PathAnyFunSpec</code>, you can
   * instead wrap them all in a <a href="../Suites.html"><code>Suites</code></a>
   * object and put them in whatever order you wish.
   * </p>
   *
   * <p>
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * </p>
   */
  final override def nestedSuites: collection.immutable.IndexedSeq[Suite] = Vector.empty

  /**
   * <strong>The <code>styleName</code> lifecycle method has been deprecated and will be removed in a future version of ScalaTest.</strong>
   *
   * <p>This method was used to support the chosen styles feature, which was deactivated in 3.1.0. The internal modularization of ScalaTest in 3.2.0
   * will replace chosen styles as the tool to encourage consistency across a project. We do not plan a replacement for <code>styleName</code>.</p>
   */
  @deprecated("The styleName lifecycle method has been deprecated and will be removed in a future version of ScalaTest with no replacement.", "3.1.0")
  final override val styleName: String = "org.scalatest.path.FunSpec"
    
  override def testDataFor(testName: String, theConfigMap: ConfigMap = ConfigMap.empty): TestData = {
    ensureTestResultsRegistered(thisSuite)
    createTestDataFor(testName, theConfigMap, this)
  }
}

