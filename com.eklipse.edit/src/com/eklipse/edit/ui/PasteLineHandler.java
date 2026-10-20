package com.eklipse.edit.ui;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.IWorkbenchCommandConstants;

import com.eklipse.edit.core.LineClipboard;

/**
 * Pastes a line copied or cut without selection above the current line when nothing is selected, pastes normally
 * otherwise.
 */
public class PasteLineHandler extends LineClipboardHandler {

	public PasteLineHandler() {
		super(IWorkbenchCommandConstants.EDIT_PASTE);
	}

	@Override
	boolean run(EditorText editorText) throws BadLocationException {
		String line = clipboardLine;
		if (line == null || !line.equals(clipboardText()) || !editorText.isEditable()) {
			return false;
		}
		editorText.moveCaret(LineClipboard.pasteLine(editorText.document(), editorText.caret(), line));
		return true;
	}
}
