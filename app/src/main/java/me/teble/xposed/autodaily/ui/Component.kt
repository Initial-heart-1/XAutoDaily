package me.teble.xposed.autodaily.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import me.teble.xposed.autodaily.R

@Composable
fun VerticaLine(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
    width: Dp = 1.dp,
    topIndent: Dp = 0.dp
) {
    val indentMod = if (topIndent.value != 0f) {
        Modifier.padding(top = topIndent)
    } else {
        Modifier
    }
    Box(
        modifier
            .then(indentMod)
            .fillMaxHeight()
            .width(width)
            .background(color = color)
    )
}

@Composable
fun AppBar(title: String, navController: NavHostController) {
    TopAppBar(
        elevation = 0.dp,
        title = {
            Text(title, color = Color.White)
        },
        backgroundColor = Color(0xFF409EFF),
        navigationIcon = {
            IconButton(onClick = {
                navController.popBackStack()
            }) {
                Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
            }
        },
        modifier = Modifier
            .background(Color(0xFF409EFF))
            .padding(
                top = navController.context
                    .getStatusBarHeightPx()
                    .toDp()
            )
    )
}


@Composable
fun ActivityView(
    title: String = "XAutoDaily",
    navController: NavHostController,
    unit: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
    ) {
        AppBar(title, navController)
        unit()
    }
}

@Composable
fun LineButton(
    modifier: Modifier = Modifier,
    title: String,
    desc: String? = null,
    otherInfoList: List<String>? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(13.dp))
            .click(onClick = onClick)
            .then(modifier)
            .padding(horizontal = 13.dp, vertical = 5.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    color = Color(0xFF424242),
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth(0.87f)
                )
                Image(
                    painter = painterResource(R.drawable.ic_right),
                    contentDescription = "",
                    modifier = Modifier
                        .width(16.dp)
                        .height(16.dp),
                )
            }
            desc?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    color = Color(0xFFC0C4CC)
                )
            }
            otherInfoList?.forEach {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    color = Color(0xFFC0C4CC)
                )
            }
        }
    }
}

@Composable
fun LineSwitch(
    modifier: Modifier = Modifier,
    checked: MutableState<Boolean>,
    title: String,
    desc: String? = null,
    otherInfoList: List<String>? = null,
    onChange: (Boolean) -> Unit = {},
    onClick: (() -> Unit)? = null,
    longPress: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(13.dp))
            .click(
                onClick = {
                    if (onClick == null) {
                        checked.value = !checked.value
                        onChange(checked.value)
                    } else {
                        onClick()
                    }
                },
                longPress = longPress
            )
            .then(modifier)
            .padding(horizontal = 13.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                color = Color(0xFF424242),
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth(0.87f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Switch(
                    checked = checked.value,
                    onCheckedChange = {
                        checked.value = it
                        onChange(it)
                    }
                )
            }
        }
        desc?.let {
            Text(
                text = it,
                fontSize = 14.sp,
                color = Color(0xFFC0C4CC)
            )
        }
        otherInfoList?.forEach {
            Text(
                text = it,
                fontSize = 12.sp,
                color = Color(0xFFFF4A4A)
            )
        }
    }
}

@Composable
fun LineCheckBox(
    modifier: Modifier = Modifier,
    checked: MutableState<Boolean>,
    title: String,
    desc: String? = null,
    otherInfoList: List<String>? = null,
    onChange: (Boolean) -> Unit = {},
    onClick: (() -> Unit)? = null,
    longPress: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .click(
                onClick = {
                    if (onClick == null) {
                        checked.value = !checked.value
                        onChange(checked.value)
                    } else {
                        onClick()
                    }
                },
                longPress = longPress
            )
            .then(modifier)
            .padding(horizontal = 13.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                color = Color(0xFF424242),
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth(0.87f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Checkbox(
                    checked = checked.value,
                    onCheckedChange = {
                        checked.value = it
                        onChange(it)
                    },
                )
            }
        }
        desc?.let {
            Text(
                text = desc,
                fontSize = 14.sp,
                color = Color(0xFFC0C4CC)
            )
        }
        otherInfoList?.forEach {
            Text(
                text = it,
                fontSize = 12.sp,
                color = Color.Red
            )
        }
    }
}

@Composable
fun Announcement(
    modifier: Modifier = Modifier,
    post: MutableState<String> = mutableStateOf(""),
) {
    if (post.value.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .then(modifier)
                .fillMaxWidth()
                .background(Color(0xFFF0F2F5))
                .background(Color.White, RoundedCornerShape(13.dp))
        ) {
            Text(
                text = "公告",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(10.dp)
                    .background(Color(0XFFF56C6C), RoundedCornerShape(50.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
            Divider(color = Color(color = 0xFFF2F2F2), thickness = 1.dp)
            Column(
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, top = 5.dp, bottom = 16.dp)
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(
                            style = ParagraphStyle(textIndent = TextIndent(firstLine = 16.sp))
                        ) {
                            withStyle(style = SpanStyle(color = Color(0xFF9A9A9A))) {
                                append(post.value)
                            }
                        }
                    },
                    fontSize = 16.sp,
                    letterSpacing = 0.2.sp,
                )
            }
        }
    }
}

@Composable
fun LineSpacer(height: Dp = 10.dp) {
    Spacer(
        modifier = Modifier
            .height(height)
            .fillMaxWidth()
    )
}


@Composable
fun GroupList(
    title: String,
    unit: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        GroupListTitle(title)
//        Divider(color = Color(color = 0xFFF2F2F2), thickness = 1.dp)
        unit()
    }
}

@Composable
fun GroupListTitle(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
//            .height(25.dp)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_v),
            contentDescription = "",
            modifier = Modifier
                .width(16.dp)
                .height(16.dp)
        )
        Text(text = text, fontSize = 16.sp)
    }
}


@SuppressLint("UnrememberedMutableState")
@Preview
@Composable
fun PreviewGroupList() {
    GroupList(title = "这是标题") {
        LineSwitch(
            title = "测试任务",
            desc = "测试任务",
            checked = mutableStateOf(true),
            onChange = {

            },
            otherInfoList = listOf("上次执行时间：2020-12-11 19:15:55")
        )
        LineCheckBox(
            title = "好友111111111111111111111111111111111111111111111",
            desc = "110",
            checked = mutableStateOf(true),
            onChange = {}
        )
        LineCheckBox(
            title = "好友12",
            desc = "119",
            checked = mutableStateOf(true),
            onChange = {}
        )
    }
}