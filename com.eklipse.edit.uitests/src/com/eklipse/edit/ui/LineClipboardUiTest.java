package com.eklipse.edit.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.BooleanSupplier;

import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Runs the commands in a text editor of a real workbench.
 */
class LineClipboardUiTest {

	private static final String TEXT = "first\nsecond\nthird\n";

	private IProject project;
	private IWorkbenchPage page;
	private ITextEditor editor;
	private StyledText widget;

	@BeforeEach
	void setUp() throws Exception {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IIntroPart intro = PlatformUI.getWorkbench().getIntroManager().getIntro();
		if (intro != null) {
			PlatformUI.getWorkbench().getIntroManager().closeIntro(intro);
		}
		page = window.getActivePage();

		IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription("demo-project");
		description.setLocation(org.eclipse.core.runtime.Path.fromOSString(Files.createTempDirectory("demo").toString()));
		project = ResourcesPlugin.getWorkspace().getRoot().getProject("demo-project");
		project.create(description, null);
		project.open(null);
		IFile file = project.getFile("notes.txt");
		file.create(new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8)), true, null);
		IDE.setDefaultEditor(file, "org.eclipse.ui.DefaultTextEditor");
		editor = (ITextEditor) IDE.openEditor(page, file);
		widget = (StyledText) editor.getAdapter(Control.class);
		// the test runtime starts in the background, without an active window nothing has the focus
		window.getShell().forceActive();
		editor.setFocus();
		waitUntil(widget::isFocusControl, 5_000);
		assertTrue(widget.isFocusControl(), "the editor's text has the focus");
		setClipboard("before");
	}

	@AfterEach
	void tearDown() throws CoreException {
		page.closeAllEditors(false);
		project.delete(true, true, null);
	}

	@Test
	void bindsCopyCutAndPasteInTextEditors() throws Exception {
		IBindingService bindings = PlatformUI.getWorkbench().getService(IBindingService.class);
		assertEquals("com.eklipse.edit.copy", boundCommand(bindings, "M1+C"));
		assertEquals("com.eklipse.edit.cut", boundCommand(bindings, "M1+X"));
		assertEquals("com.eklipse.edit.paste", boundCommand(bindings, "M1+V"));
	}

	@Test
	void copiesTheLineWhenNothingIsSelected() throws Exception {
		editor.selectAndReveal(8, 0);
		execute("com.eklipse.edit.copy");
		assertEquals("second\n", clipboard());
		assertEquals(TEXT, document().get());
		assertEquals(8, selection().getOffset(), "the caret stays");
		assertEquals(0, selection().getLength());
	}

	@Test
	void copiesTheSelection() throws Exception {
		editor.selectAndReveal(8, 4);
		execute("com.eklipse.edit.copy");
		assertEquals("cond", clipboard());
	}

	@Test
	void cutsTheLineWhenNothingIsSelected() throws Exception {
		editor.selectAndReveal(8, 0);
		execute("com.eklipse.edit.cut");
		assertEquals("second\n", clipboard());
		assertEquals("first\nthird\n", document().get());
		assertEquals(8, selection().getOffset(), "the caret keeps its column");

		execute(IWorkbenchCommandConstants.EDIT_UNDO);
		assertEquals(TEXT, document().get(), "one undo brings the line back");
	}

	@Test
	void cutsTheSelection() throws Exception {
		editor.selectAndReveal(8, 4);
		execute("com.eklipse.edit.cut");
		assertEquals("cond", clipboard());
		assertEquals("first\nse\nthird\n", document().get());
	}

	@Test
	void pastesACopiedLineAboveTheCurrentLine() throws Exception {
		editor.selectAndReveal(2, 0);
		execute("com.eklipse.edit.copy");
		editor.selectAndReveal(15, 0);
		execute("com.eklipse.edit.paste");
		assertEquals("first\nsecond\nfirst\nthird\n", document().get());
		assertEquals(21, selection().getOffset(), "the caret stays at the same text");
	}

	@Test
	void pastesACopiedLineOverASelectionAsText() throws Exception {
		editor.selectAndReveal(2, 0);
		execute("com.eklipse.edit.copy");
		editor.selectAndReveal(13, 5);
		execute("com.eklipse.edit.paste");
		assertEquals("first\nsecond\nfirst\n\n", document().get());
	}

	@Test
	void pastesOtherTextAtTheCaret() throws Exception {
		editor.selectAndReveal(2, 0);
		execute("com.eklipse.edit.copy");
		setClipboard("word");
		editor.selectAndReveal(8, 0);
		execute("com.eklipse.edit.paste");
		assertEquals("first\nsewordcond\nthird\n", document().get());
	}

	@Test
	void leavesTextFieldsInsideTheEditorAlone() throws Exception {
		// like the find overlay
		Text field = new Text(widget.getParent(), SWT.SINGLE);
		try {
			field.setText("field");
			field.setFocus();
			processEvents();
			assertTrue(field.isFocusControl());

			execute("com.eklipse.edit.copy");
			assertEquals("before", clipboard(), "no line copied");
			execute("com.eklipse.edit.cut");
			assertEquals(TEXT, document().get(), "no line cut");
		} finally {
			field.dispose();
		}
	}

	private static String boundCommand(IBindingService bindings, String keys) throws Exception {
		Binding binding = bindings.getPerfectMatch(KeySequence.getInstance(keys));
		assertNotNull(binding, keys + " is bound");
		return binding.getParameterizedCommand().getId();
	}

	private static void execute(String commandId) throws CommandException {
		PlatformUI.getWorkbench().getService(IHandlerService.class).executeCommand(commandId, null);
		processEvents();
	}

	private IDocument document() {
		return editor.getDocumentProvider().getDocument(editor.getEditorInput());
	}

	private ITextSelection selection() {
		return (ITextSelection) editor.getSelectionProvider().getSelection();
	}

	private static String clipboard() {
		Clipboard clipboard = new Clipboard(Display.getCurrent());
		try {
			return (String) clipboard.getContents(TextTransfer.getInstance());
		} finally {
			clipboard.dispose();
		}
	}

	private static void setClipboard(String text) {
		Clipboard clipboard = new Clipboard(Display.getCurrent());
		try {
			clipboard.setContents(new Object[] { text }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
	}

	private static void processEvents() {
		Display display = Display.getCurrent();
		while (display.readAndDispatch()) {
			// keep going
		}
	}

	private static void waitUntil(BooleanSupplier condition, long timeoutMs) {
		Display display = Display.getCurrent();
		long end = System.currentTimeMillis() + timeoutMs;
		while (!condition.getAsBoolean() && System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				display.timerExec(20, () -> {
					// wake up
				});
				display.sleep();
			}
		}
	}
}
