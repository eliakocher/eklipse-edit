package com.eklipse.edit.ui;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.IWorkbenchCommandConstants;

import com.eklipse.edit.core.LineClipboard;

/**
 * Copies the current line when nothing is selected, the selection otherwise.
 */
public class CopyLineHandler extends LineClipboardHandler {

	public CopyLineHandler() {
		super(IWorkbenchCommandConstants.EDIT_COPY);
	}

	@Override
	boolean run(EditorText editorText) throws BadLocationException {
		putOnClipboard(LineClipboard.lineText(editorText.document(), editorText.caret()));
		return true;
	}
}
