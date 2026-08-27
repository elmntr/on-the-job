package com.example.onthejob.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onthejob.data.ojt.OjtInstance
import com.example.onthejob.ui.theme.*

@Composable
fun OjtInstancePicker(
    activeInstance: OjtInstance?,
    instances: List<OjtInstance>,
    onSelectInstance: (String) -> Unit,
    onNewInstanceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(vertical = 4.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = activeInstance?.name ?: "OJT 1",
                fontFamily = CondFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Ink,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Switch OJT instance",
                tint = Ink,
                modifier = Modifier.size(20.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CardSurface, RoundedCornerShape(12.dp)),
        ) {
            instances.forEach { instance ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (instance.id == activeInstance?.id) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = AmberDark,
                                    modifier = Modifier.size(16.dp),
                                )
                            } else {
                                Spacer(Modifier.size(16.dp))
                            }
                            Text(
                                instance.name,
                                fontFamily = BodyFontFamily,
                                fontSize = 13.sp,
                                color = Ink,
                            )
                        }
                    },
                    onClick = {
                        onSelectInstance(instance.id)
                        expanded = false
                    },
                )
            }

            HorizontalDivider(color = Line)

            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            "New OJT instance…",
                            fontFamily = CondFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Ink,
                        )
                    }
                },
                onClick = {
                    expanded = false
                    onNewInstanceClick()
                },
            )
        }
    }
}
