package com.wolfesoftware.mipsos.assembler;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

public class AssemblerDemo extends JApplet
{
    // GUI
    private JSplitPane spnEditor;
    private JPanel pnlSrc;
    private JLabel lblSrc;
    private JTextArea txaSrc;
    private JPanel pnlOut;
    private JLabel lblOut;
    private JTextArea txaOut;
    private JPanel pnlControls;
    private JButton btnAssem;

    // init
    @Override
    public void init()
    {
        // Applet
        Dimension d = this.getSize();
        d.height = Math.max(d.height, 500);
        d.width = Math.max(d.width, 500);
        this.setSize(d);

        // GUI
        final Font codeFont = new Font("Courier New", Font.PLAIN, 12);
        final int tabSize = 8;
        Container rootContainer = this.getContentPane();
        rootContainer.setLayout(new BorderLayout());
        spnEditor = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        pnlSrc = new JPanel(new BorderLayout());
        lblSrc = new JLabel("Source code:");
        pnlSrc.add(lblSrc, BorderLayout.NORTH);
        txaSrc = new JTextArea("# Mips source goes here\n");
        txaSrc.setFont(codeFont);
        txaSrc.setTabSize(tabSize);
        pnlSrc.add(new JScrollPane(txaSrc), BorderLayout.CENTER);
        spnEditor.add(pnlSrc);
        pnlOut = new JPanel(new BorderLayout());
        lblOut = new JLabel("Output:");
        pnlOut.add(lblOut, BorderLayout.NORTH);
        txaOut = new JTextArea();
        txaOut.setFont(codeFont);
        txaOut.setTabSize(tabSize);
        pnlOut.add(new JScrollPane(txaOut), BorderLayout.CENTER);
        spnEditor.add(pnlOut);
        spnEditor.setResizeWeight(0.5);
        rootContainer.add(spnEditor, BorderLayout.CENTER);
        pnlControls = new JPanel(new FlowLayout());
        btnAssem = new JButton("Assemble!");
        btnAssem.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAssem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                assemble();
            }
        });
        pnlControls.add(btnAssem);
        rootContainer.add(pnlControls, BorderLayout.SOUTH);
    }

    private void assemble()
    {
        // prepare streams and assembler options
        InputStream inStream = new ByteArrayInputStream(txaSrc.getText().getBytes());
        java.util.Map<String, Integer> baseAddresses = Assembler.makeDefaultOptions().getSegmentBaseAddresses();

        OutputStream outStream = new ByteArrayOutputStream();
        // assembler and catch errors
        try {
            Assembler.assemble(inStream, outStream, true, baseAddresses.get(".data"), baseAddresses.get(".text"));
            // success
            txaOut.setText(outStream.toString());
        } catch (CompilingException e) {
            // error that can be pointed to in the source
            txaOut.setText(e.getMessage());
            txaSrc.select(e.srcStart, e.srcStart + e.len);
            txaSrc.grabFocus();
        } catch (AssemblingException e) {
            // error that can't be pointed to in the source
            txaOut.setText(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
