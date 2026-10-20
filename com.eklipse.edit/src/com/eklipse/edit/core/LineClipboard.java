package com.eklipse.edit.core;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;

/**
 * Copies, cuts and pastes whole lines of a document, like VS Code does when nothing is selected.
 */
public final class LineClipboard {

	private LineClipboard() {
	}

	/**
	 * Returns the line at the offset with its line delimiter. The last line has none, it gets the document's default
	 * one, so that every copied line pastes the same way.
	 *
	 * @param document the document
	 * @param offset an offset on the line
	 * @return the line's text, ending with a line delimiter
	 * @throws BadLocationException if the offset isn't in the document
	 */
	public static String lineText(IDocument document, int offset) throws BadLocationException {
		int line = document.getLineOfOffset(offset);
		IRegion region = document.getLineInformation(line);
		String delimiter = document.getLineDelimiter(line);
		return document.get(region.getOffset(), region.getLength())
				+ (delimiter != null ? delimiter : TextUtilities.getDefaultLineDelimiter(document));
	}

	/**
	 * Removes the line at the offset with a line delimiter: its own, or for the last line the one before it.
	 *
	 * @param document the document
	 * @param offset an offset on the line
	 * @return the caret offset afterwards, in the same column of the line taking the removed one's place, or at its
	 *         end if it's shorter
	 * @throws BadLocationException if the offset isn't in the document
	 */
	public static int cutLine(IDocument document, int offset) throws BadLocationException {
		int line = document.getLineOfOffset(offset);
		int lineOffset = document.getLineOffset(line);
		int column = offset - lineOffset;
		int start = lineOffset;
		if (document.getLineDelimiter(line) == null && line > 0) {
			start -= document.getLineDelimiter(line - 1).length();
		}
		document.replace(start, lineOffset + document.getLineLength(line) - start, "");

		int caretLine = Math.min(line, document.getNumberOfLines() - 1);
		IRegion caretRegion = document.getLineInformation(caretLine);
		return caretRegion.getOffset() + Math.min(column, caretRegion.getLength());
	}

	/**
	 * Inserts a line copied with {@link #lineText(IDocument, int)} above the line at the offset. Its line delimiter is
	 * replaced by the document's default one.
	 *
	 * @param document the document
	 * @param offset an offset on the line
	 * @param text the line, with or without a line delimiter
	 * @return the caret offset afterwards, at the same text as before
	 * @throws BadLocationException if the offset isn't in the document
	 */
	public static int pasteLine(IDocument document, int offset, String text) throws BadLocationException {
		String[] delimiters = document.getLegalLineDelimiters();
		int delimiter = TextUtilities.endsWith(delimiters, text);
		String line = (delimiter < 0 ? text : text.substring(0, text.length() - delimiters[delimiter].length()))
				+ TextUtilities.getDefaultLineDelimiter(document);
		document.replace(document.getLineInformationOfOffset(offset).getOffset(), 0, line);
		return offset + line.length();
	}
}
