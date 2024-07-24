package org.jetbrains.plugins.featurefilegenerator

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.*
import kotlin.math.absoluteValue
import kotlin.random.Random

class UIPanel(private val project: com.intellij.openapi.project.Project) : DialogWrapper(true) {
    val userStoryPathField = JTextField()
    val outputDirPathField = JTextField()
    val debugCheckBox = JCheckBox("Debug")
    val temperatureSpinner = JSpinner(SpinnerNumberModel(0.0, 0.0, 2.0, 0.1)).apply {
        editor = JSpinner.NumberEditor(this, "1.0")
    }
    val apiKeyField = JTextField().apply {
        preferredSize = java.awt.Dimension(300, preferredSize.height)
    }
    val seedSpinner = JSpinner(SpinnerNumberModel(Random.nextInt().absoluteValue, 0, Integer.MAX_VALUE, 1)).apply {
        preferredSize = java.awt.Dimension(200, preferredSize.height)
    }

    init {
        init()
        title = "Generate Feature File"
    }

    override fun createCenterPanel(): JComponent? {
        return panel {
            row("Api Key:") {
                cell(apiKeyField)
                    .horizontalAlign(HorizontalAlign.FILL)
                cell(createTooltipLabel("Enter OpenAI API key here"))
            }
            row("User Story file:") {
                cell(userStoryPathField)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .resizableColumn()
                button("Explore") {
                    val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
                    val file = FileChooser.chooseFile(descriptor, project, null)
                    file?.let { userStoryPathField.text = it.path }
                }
                cell(createTooltipLabel("Select the user story file"))
            }
            row("Output Directory:") {
                cell(outputDirPathField)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .resizableColumn()
                button("Explore") {
                    val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
                    val file = FileChooser.chooseFile(descriptor, project, null)
                    file?.let { outputDirPathField.text = it.path }
                }
                cell(createTooltipLabel("Select the output directory"))
            }

            row("Seed:") {
                cell(seedSpinner)
                    .horizontalAlign(HorizontalAlign.FILL)
                cell(createTooltipLabel("Default: Random Int+. Enter a seed value if you want semi-reproducible results"))
            }
            row("Temperature:") {
                cell(temperatureSpinner)
                cell(createTooltipLabel("Set the temperature for the model (default is 1)"))
            }
            row {
                cell(debugCheckBox)
                    .horizontalAlign(HorizontalAlign.LEFT)
                cell(createTooltipLabel("Enable debug mode"))
            }
        }
    }

    private fun createTooltipLabel(tooltipText: String): JLabel {
        return JLabel("\u2753").apply {
            toolTipText = tooltipText
        }
    }
}
