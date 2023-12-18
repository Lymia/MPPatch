/*
 * Copyright (c) 2015-2023 Lymia Kanokawa <lymia@lymia.moe>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package moe.lymia.mppatch.ui;

import com.intellij.uiDesigner.core.GridLayoutManager;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.Locale;

public class FormChooseInstallation implements FormPanel {

    private JPanel mainPane;
    private JButton selectDirectoryButton;
    private JButton addDirectoryButton;
    private JScrollPane installationListPane;
    private JPanel installationList;

    public JComponent getRootComponent() {
        return mainPane;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPane = new JPanel();
        mainPane.setLayout(new FormLayout("fill:max(d;4px):noGrow,left:d:noGrow,fill:max(d;4px):noGrow,left:p:noGrow,fill:d:grow,left:d:noGrow,fill:max(d;4px):noGrow", "center:max(d;4px):noGrow,top:d:noGrow,center:max(d;4px):noGrow,top:d:noGrow,center:19px:noGrow,top:3dlu:noGrow,center:m:grow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:d:noGrow,center:max(d;4px):noGrow"));
        mainPane.putClientProperty("html.disable", Boolean.FALSE);
        mainPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        installationListPane = new JScrollPane();
        installationListPane.setVerticalScrollBarPolicy(22);
        CellConstraints cc = new CellConstraints();
        mainPane.add(installationListPane, cc.xyw(3, 7, 3, CellConstraints.FILL, CellConstraints.FILL));
        installationList = new JPanel();
        installationList.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        installationListPane.setViewportView(installationList);
        final JLabel label1 = new JLabel();
        label1.setEnabled(true);
        Font label1Font = this.$$$getFont$$$(null, Font.BOLD, 16, label1.getFont());
        if (label1Font != null) label1.setFont(label1Font);
        label1.setText("Choose Installation");
        mainPane.add(label1, cc.xyw(3, 3, 3));
        final JLabel label2 = new JLabel();
        label2.setText("Please select the correct Civilization V installation directory.");
        mainPane.add(label2, cc.xyw(3, 5, 3));
        selectDirectoryButton = new JButton();
        selectDirectoryButton.setText("Select Directory");
        mainPane.add(selectDirectoryButton, cc.xy(5, 9));
        addDirectoryButton = new JButton();
        addDirectoryButton.setText("Add Directory");
        mainPane.add(addDirectoryButton, cc.xy(3, 9));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPane;
    }

}
