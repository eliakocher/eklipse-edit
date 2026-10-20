package com.eklipse.edit.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IBlockTextSelection;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IMultiTextSelection;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension2;

/**
 * Base of the Copy, Cut and Paste commands bound in text editors. They act on the whole line when the editor's text has
 * the focus and nothing is selected, and run the standard command otherwise: the editors' own clipboard actions stay in
 * charge of selections, JDT's copy with imports for instance, and so do text fields inside editors, like the find
 * overlay.
 */
abstract class LineClipboardHandler extends AbstractHandler {

	/** The last line put on the clipboard; Paste inserts it above the current line while it's still there. */
	static String clipboardLine;

	private final String standardCommandId;

	LineClipboardHandler(String standardCommandId) {
		this.standardCommandId = standardCommandId;
	}

	/**
	 * Acts on the line of the caret.
	 *
	 * @param editorText the focused editor text, with a caret and no selection
	 * @return {@code false} to run the standard command instead
	 * @throws BadLocationException if the document changed meanwhile
	 */
	abstract boolean run(EditorText editorText) throws BadLocationException;

	/**
	 * Lets the key go to the focused widget, as with the standard key binding, when the standard command isn't handled.
	 * Text fields rely on that, like the find overlay of text editors, which removes the editor's clipboard actions
	 * while it has the focus.
	 */
	@Override
	public boolean isHandled() {
		return EditorText.focused() != null || standardCommand().isHandled();
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		EditorText editorText = EditorText.focused();
		try {
			if (editorText != null && editorText.caret() >= 0 && run(editorText)) {
				return null;
			}
		} catch (BadLocationException e) {
			throw new ExecutionException("Could not change the line", e);
		}
		try {
			IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
			handlerService.executeCommand(standardCommandId,
					event.getTrigger() instanceof Event trigger ? trigger : null);
		} catch (NotDefinedException | NotEnabledException | NotHandledException e) {
			// nothing to copy, cut or paste, like with the standard key binding
		}
		return null;
	}

	private Command standardCommand() {
		return PlatformUI.getWorkbench().getService(ICommandService.class).getCommand(standardCommandId);
	}

	static void putOnClipboard(String line) {
		Clipboard clipboard = new Clipboard(Display.getCurrent());
		try {
			clipboard.setContents(new Object[] { line }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
		clipboardLine = line;
	}

	static String clipboardText() {
		Clipboard clipboard = new Clipboard(Display.getCurrent());
		try {
			return (String) clipboard.getContents(TextTransfer.getInstance());
		} finally {
			clipboard.dispose();
		}
	}

	/**
	 * The text viewer of the active editor, while its text has the focus.
	 */
	record EditorText(IWorkbenchPart part, ITextViewer viewer) {

		static EditorText focused() {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page = window != null ? window.getActivePage() : null;
			IWorkbenchPart part = page != null ? page.getActivePart() : null;
			// multi-page editors adapt to the viewer of their active page
			ITextViewer viewer = part != null ? Adapters.adapt(part, ITextViewer.class) : null;
			StyledText widget = viewer != null ? viewer.getTextWidget() : null;
			return widget != null && !widget.isDisposed() && widget.isFocusControl() ? new EditorText(part, viewer)
					: null;
		}

		IDocument document() {
			return viewer.getDocument();
		}

		/**
		 * @return the caret offset, or -1 if text is selected or there are several carets
		 */
		int caret() {
			ISelection selection = viewer.getSelectionProvider().getSelection();
			if (document() == null || selection instanceof IBlockTextSelection
					|| (selection instanceof IMultiTextSelection multi && multi.getRegions().length > 1)
					|| !(selection instanceof ITextSelection text) || text.getLength() > 0) {
				return -1;
			}
			return text.getOffset();
		}

		/**
		 * Asks the editor first, like its own actions do, it may make a read-only file writable.
		 */
		boolean isEditable() {
			ITextEditor editor = Adapters.adapt(part, ITextEditor.class);
			if (editor instanceof ITextEditorExtension2 extension && !extension.validateEditorInputState()) {
				return false;
			}
			return viewer.isEditable();
		}

		void moveCaret(int offset) {
			viewer.setSelectedRange(offset, 0);
			viewer.revealRange(offset, 0);
		}
	}
}
