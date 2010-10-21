package com.wolfesoftware.mipsos.mips.simulator;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.wolfesoftware.mipsos.common.*;

public class SimulatorDemo extends JApplet
{
    private static final long serialVersionUID = -4484450125132914048L;

    // GUI
    private JTextArea txaOutput;
    private JPanel pnlControls;
    private JButton btnReset;
    private JButton btnRunBreakResume;
    private JPanel pnlInput;
    private JTextField txfInput;
    private JButton btnInput;

    // Simulator
    private ISimulatorCore simCore;
    private SimRunner simRunner;
    private Thread simRunnerThread;
    private EStatus status;

    public SimulatorDemo()
    {
        simCore = getSimulaotrCore();
        simCore.setSimulatorListener(new ISimulatorListener() {
            public void output(String outText)
            {
                handleOutput(outText);
            }
        });

        simRunner = new SimRunner();
        simRunnerThread = new Thread(simRunner);
        status = EStatus.Break;
    }

    /** Overridable or something */
    protected ISimulatorCore getSimulaotrCore()
    {
        return new SimulatorCore();
    }

    public void init()
    {
        Container appletPane = getContentPane();
        appletPane.setLayout(new BorderLayout());
        txaOutput = new JTextArea();
        appletPane.add(txaOutput, BorderLayout.CENTER);
        pnlControls = new JPanel(new GridLayout(2, 1));
        btnReset = new JButton("Reset");
        pnlControls.add(btnReset);
        btnRunBreakResume = new JButton("Run");
        pnlControls.add(btnRunBreakResume);
        appletPane.add(pnlControls, BorderLayout.EAST);
        pnlInput = new JPanel(new BorderLayout());
        txfInput = new JTextField();
        pnlInput.add(txfInput, BorderLayout.CENTER);
        btnInput = new JButton(">");
        pnlInput.add(btnInput, BorderLayout.EAST);
        appletPane.add(pnlInput, BorderLayout.SOUTH);

        btnReset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                handleReset();
            }
        });

        btnRunBreakResume.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                handleRunBreakResume();
            }
        });
    }

    private void handleOutput(String outText)
    {
        txaOutput.append(outText); // thread safe
    }

    private void handleReset()
    {

    }

    private void handleRunBreakResume()
    {
        switch (status)
        {
            case Break:
                setStatus(EStatus.Ready);
                setRunnerEnabled(true);
                break;
            case Ready: // running. needs to be broken
                handleBreak(EStatus.Break);
                break;
            default:
                throw new RuntimeException(); // TODO
        }
    }

    private void handleBreak(EStatus status)
    {
        setRunnerEnabled(false);
        switch (status)
        {
            case Break: // harmless break

        }
    }

    private void setStatus(EStatus status)
    {
        switch (status)
        {
            case Break:
                btnRunBreakResume.setText("Resume");
                break;
            case Done:
                btnRunBreakResume.setText("Stopped");
                btnRunBreakResume.setEnabled(false);
                break;
            case NeedInput:
                btnRunBreakResume.setText("Input");
                btnRunBreakResume.setEnabled(false);
                break;
            case Ready:
                btnRunBreakResume.setText("Break");
                break;
        }
    }

    private void setRunnerEnabled(boolean en)
    {
        if (en)
        {
            if (simRunnerThread != null && simRunnerThread.isAlive())
                setRunnerEnabled(false);
            simRunnerThread = new Thread(simRunner);
            simRunnerThread.start();
        } else
        {
            if (simRunnerThread == null)
                return;
            simRunner.setBreak();
            while (simRunnerThread.isAlive())
            {
                try
                {
                    Thread.sleep(1);
                } catch (InterruptedException e)
                {
                    // Thread.interrupt() should never be used
                }
            }
        }
    }

    private void resyncBreak(final EStatus status)
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                handleBreak(status);
            }
        });
    }

    private class SimRunner implements Runnable
    {
        private boolean breakMe = false;

        public void setBreak()
        {
            breakMe = true;
        }

        public void run()
        {
            EStatus status = null;
            boolean keepLooping = true;
            while (keepLooping)
            {
                if (breakMe)
                {
                    breakMe = false;
                    status = EStatus.Break;
                    break;
                }
                status = simCore.step();
                switch (status)
                {
                    case Ready:
                        continue;
                    case Break:
                    case NeedInput:
                    case Done:
                        keepLooping = false;
                        break;
                    default:
                        throw new RuntimeException(); // TODO
                }
            }
            resyncBreak(status);
        }
    }

}
