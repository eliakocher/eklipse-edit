package com.eklipse.edit.ui;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.IWorkbenchCommandConstants;

import com.eklipse.edit.core.LineClipboard;

/**
 * Cuts the current line when nothing is selected, the selection otherwise.
 */
public class CutLineHandler extends LineClipboardHandler {

	public CutLineHandler() {
		super(IWorkbenchCommandConstants.EDIT_CUT);
	}

	@Override
	boolean run(EditorText editorText) throws BadLocationException {
		if (!editorText.isEditable()) {
			return false;
		}
		int caret = editorText.caret();
		putOnClipboard(LineClipboard.lineText(editorText.document(), caret));
		editorText.moveCaret(LineClipboard.cutLine(editorText.document(), caret));
		return true;
	}
}
