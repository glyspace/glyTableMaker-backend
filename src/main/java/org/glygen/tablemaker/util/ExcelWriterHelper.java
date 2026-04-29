package org.glygen.tablemaker.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.poi.examples.ss.AddDimensionedImage;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.ClientAnchor.AnchorType;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.Units;

public class ExcelWriterHelper extends AddDimensionedImage {
	/**
	 * 
	 * Writes the given image object into a cell in the given workbook and sheet 
	 * at the given cell identified by row and column indexes
	 * 
	 * @see {@link org.apache.poi.ss.examples.AddDimensionedImage.addImageToSheet()}
	 * @param a_workbook Excel workbook
	 * @param a_sheet sheet to use
	 * @param a_iRowNum row number for the cell
	 * @param a_iColNum column number for the cell
	 * @param a_img image to add to the given cell
	 * @param a_imgs array of images to put the newly generated excel picture into (to be used for resizing the images later)
	 */
	public void writeCellImage(Workbook workbook, Sheet sheet, int rowNum, int colNum, 
			BufferedImage image, List<Picture> a_imgs) throws Exception {
		
		if (colNum < 0 || image == null) {
	        return;
	    }
		
		int paddingPx = 10;

		// Make the cell larger than the image
		sheet.setColumnWidth(colNum,
		    (int)((image.getWidth() + paddingPx * 2) * 256 / 7.0));

		Row row = sheet.getRow(rowNum);
		if (row == null) row = sheet.createRow(rowNum);
		row.setHeightInPoints((image.getHeight() + paddingPx * 2) * 0.75f);
		
		Drawing<?> drawing =
		        sheet.getDrawingPatriarch() != null
		            ? sheet.getDrawingPatriarch()
		            : sheet.createDrawingPatriarch();

		ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();

		anchor.setCol1(colNum);
		anchor.setRow1(rowNum);
		anchor.setCol2(colNum);
		anchor.setRow2(rowNum);
		
		// calculate limits
		row = sheet.getRow(rowNum);
		float rowPx = row.getHeightInPoints() / 0.75f;
		float colPx = sheet.getColumnWidth(colNum) / 256f * 7f;

		int padEMU = Units.pixelToEMU(8);
		int maxDx = Units.pixelToEMU((int)colPx) - padEMU;
		int maxDy = Units.pixelToEMU((int)rowPx) - padEMU;

		anchor.setDx1(padEMU);
		anchor.setDy1(padEMU);
		anchor.setDx2(Math.min(Units.pixelToEMU(image.getWidth()), maxDx));
		anchor.setDy2(Math.min(Units.pixelToEMU(image.getHeight()), maxDy));
		anchor.setAnchorType(AnchorType.MOVE_AND_RESIZE);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    ImageIO.write(image, "png", bos);
		int pictureIndex = workbook.addPicture(
	            bos.toByteArray(),
	            Workbook.PICTURE_TYPE_PNG
	    );

		Picture picture = drawing.createPicture(anchor, pictureIndex);
		a_imgs.add(picture);
	}
	
	public void resizeColumnsRows (Sheet sheet, List<Picture> pictures) throws IOException {
		for (Picture pic : pictures) {
		    ClientAnchor a = (ClientAnchor) pic.getAnchor();
		
		    int row = a.getRow1();
		    int col = a.getCol1();
		
		    BufferedImage img = ImageIO.read(
				    new ByteArrayInputStream(
				        pic.getPictureData().getData()
				    ));
			Row r = sheet.getRow(row);
		    if (r != null) {
		        r.setHeightInPoints(
		            Math.max(r.getHeightInPoints(), img.getHeight() * 0.75f)
		        );
		    }

		    sheet.setColumnWidth(
		        col,
		        Math.max(
		            sheet.getColumnWidth(col),
		            (int)(img.getWidth() * 256 / 7.0)
		        )
		    );
		}
	}
}
